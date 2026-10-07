package br.com.portalmanager.platform.library.testing.cloud.azure;

import com.azure.core.http.HttpMethod;
import com.azure.core.http.HttpPipelineCallContext;
import com.azure.core.http.HttpPipelineNextPolicy;
import com.azure.core.http.HttpPipelinePosition;
import com.azure.core.http.HttpResponse;
import com.azure.core.http.policy.HttpPipelinePolicy;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

final class AzureBlobCreatedEventPolicy implements HttpPipelinePolicy, AutoCloseable {

    private static final String TOPIC =
            "/subscriptions/00000000-0000-0000-0000-000000000000"
                    + "/resourceGroups/platform-testing/providers/Microsoft.Storage"
                    + "/storageAccounts/devstoreaccount1";

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final ServiceBusSenderClient sender;
    private final ConcurrentMap<String, Long> stagedBlockSizes = new ConcurrentHashMap<>();
    private final AtomicLong sequencer = new AtomicLong();

    AzureBlobCreatedEventPolicy(ServiceBusSenderClient sender) {
        this.sender = sender;
    }

    @Override
    public HttpPipelinePosition getPipelinePosition() {
        // Observe the final response after the SDK has exhausted any retries.
        return HttpPipelinePosition.PER_CALL;
    }

    @Override
    public Mono<HttpResponse> process(HttpPipelineCallContext context, HttpPipelineNextPolicy next) {
        var request = context.getHttpRequest();
        return next.process().doOnNext(response -> publishAfterSuccessfulUpload(request, response));
    }

    private void publishAfterSuccessfulUpload(
            com.azure.core.http.HttpRequest request,
            HttpResponse response) {
        if (!HttpMethod.PUT.equals(request.getHttpMethod())) {
            return;
        }

        Upload upload = upload(request);
        if (upload == null) {
            return;
        }

        boolean successful = response.getStatusCode() >= 200 && response.getStatusCode() < 300;
        if (!successful) {
            if ("blocklist".equals(upload.component())) {
                stagedBlockSizes.remove(upload.resourceKey());
            }
            return;
        }

        if ("block".equals(upload.component())) {
            Long size = contentLength(request);
            if (size != null) {
                stagedBlockSizes.merge(upload.resourceKey(), size, Long::sum);
            }
            return;
        }
        if ("blocklist".equals(upload.component())) {
            publish(upload, request, response, stagedBlockSizes.remove(upload.resourceKey()));
            return;
        }
        if (upload.component().isEmpty()) {
            publish(upload, request, response, contentLength(request));
        }
    }

    private Upload upload(com.azure.core.http.HttpRequest request) {
        try {
            URI uri = request.getUrl().toURI();
            String[] segments = uri.getRawPath().split("/");
            if (segments.length < 4) {
                return null;
            }

            String account = decode(segments[1]);
            String container = decode(segments[2]);
            String blob = decode(String.join("/", java.util.Arrays.copyOfRange(segments, 3, segments.length)));
            if (account.isBlank() || container.isBlank() || blob.isBlank()) {
                return null;
            }
            String component = queryParameter(uri.getRawQuery(), "comp");
            return new Upload(uri.toString(), container, blob, component,
                    account + "/" + container + "/" + blob);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to inspect Azure Blob upload request", exception);
        }
    }

    private void publish(
            Upload upload,
            com.azure.core.http.HttpRequest request,
            HttpResponse response,
            Long contentLength) {
        String eventId = UUID.randomUUID().toString();
        String requestId = response.getHeaderValue("x-ms-request-id");
        String contentType = request.getHeaders().getValue("x-ms-blob-content-type");
        if (contentType == null) {
            contentType = request.getHeaders().getValue("Content-Type");
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("api", "blocklist".equals(upload.component()) ? "PutBlockList" : "PutBlob");
        putIfPresent(data, "clientRequestId", request.getHeaders().getValue("x-ms-client-request-id"));
        putIfPresent(data, "requestId", requestId);
        putIfPresent(data, "eTag", response.getHeaderValue("ETag"));
        putIfPresent(data, "contentType", contentType);
        if (contentLength != null) {
            data.put("contentLength", contentLength);
        }
        data.put("blobType", "BlockBlob");
        data.put("url", upload.url());
        data.put("sequencer", "%032x".formatted(sequencer.incrementAndGet()));
        if (requestId != null) {
            data.put("storageDiagnostics", Map.of("batchId", requestId));
        }

        String subject = "/blobServices/default/containers/" + upload.container()
                + "/blobs/" + upload.blob();
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("topic", TOPIC);
        event.put("subject", subject);
        event.put("eventType", "Microsoft.Storage.BlobCreated");
        event.put("eventTime", OffsetDateTime.now(ZoneOffset.UTC).toString());
        event.put("id", eventId);
        event.put("data", data);
        event.put("dataVersion", "");
        event.put("metadataVersion", "1");

        try {
            String body = JSON.writeValueAsString(List.of(event));
            ServiceBusMessage message = new ServiceBusMessage(body);
            message.setContentType("application/json; charset=utf-8");
            message.getApplicationProperties().put("aeg-subscription-name", "platform-testing-blob-created");
            message.getApplicationProperties().put("aeg-delivery-count", 1);
            message.getApplicationProperties().put("aeg-event-type", "Notification");
            message.getApplicationProperties().put("aeg-metadata-version", "1");
            message.getApplicationProperties().put("aeg-data-version", "");
            message.getApplicationProperties().put("aeg-output-event-id", eventId);
            sender.sendMessage(message);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Unable to serialize Azure BlobCreated event", exception);
        }
    }

    private Long contentLength(com.azure.core.http.HttpRequest request) {
        String value = request.getHeaders().getValue("Content-Length");
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String queryParameter(String query, String name) {
        if (query == null || query.isBlank()) {
            return "";
        }
        for (String parameter : query.split("&")) {
            String[] pair = parameter.split("=", 2);
            if (decode(pair[0]).equalsIgnoreCase(name)) {
                return pair.length == 1 ? "" : decode(pair[1]);
            }
        }
        return "";
    }

    private String decode(String value) {
        return URLDecoder.decode(value.replace("+", "%2B"), StandardCharsets.UTF_8);
    }

    private void putIfPresent(Map<String, Object> target, String key, String value) {
        if (value != null && !value.isBlank()) {
            target.put(key, value);
        }
    }

    @Override
    public void close() {
        sender.close();
    }

    private record Upload(String url, String container, String blob, String component, String resourceKey) {
    }
}
