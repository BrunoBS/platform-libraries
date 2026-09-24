package br.com.portalmanager.platform.messaging.exception;

import br.com.portalmanager.platform.messaging.model.ValidationDetail;

import java.util.List;

public interface ValidationDetailsProvider {
    List<ValidationDetail> getDetails();
}
