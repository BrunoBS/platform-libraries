package br.com.portalmanager.platform.library.messaging.api.exception;

import br.com.portalmanager.platform.library.messaging.api.model.ValidationDetail;

import java.util.List;

public interface ValidationDetailsProvider {
    List<ValidationDetail> getDetails();
}
