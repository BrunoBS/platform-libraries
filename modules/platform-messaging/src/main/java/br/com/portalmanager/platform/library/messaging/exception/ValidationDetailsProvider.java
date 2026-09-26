package br.com.portalmanager.platform.library.messaging.exception;

import br.com.portalmanager.platform.library.messaging.model.ValidationDetail;

import java.util.List;

public interface ValidationDetailsProvider {
    List<ValidationDetail> getDetails();
}
