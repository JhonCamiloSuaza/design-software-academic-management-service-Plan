package co.sena.academic.domain.exception;

import java.util.UUID;

public class FichaCapacityBelowActiveLearnersException extends RuntimeException {

    public FichaCapacityBelowActiveLearnersException(
            UUID id, int requestedCapacity, int activeLearners) {
        super("Ficha " + id + " cannot set max_capacity to " + requestedCapacity
                + " because it has " + activeLearners + " active learners");
    }
}
