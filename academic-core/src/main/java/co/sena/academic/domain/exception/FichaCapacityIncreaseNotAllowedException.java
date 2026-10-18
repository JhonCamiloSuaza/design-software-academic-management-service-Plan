package co.sena.academic.domain.exception;

import java.util.UUID;

public class FichaCapacityIncreaseNotAllowedException extends RuntimeException {

    public FichaCapacityIncreaseNotAllowedException(UUID id, int currentCapacity, int requestedCapacity) {
        super("Ficha " + id + " cannot increase max_capacity from "
                + currentCapacity + " to " + requestedCapacity);
    }
}
