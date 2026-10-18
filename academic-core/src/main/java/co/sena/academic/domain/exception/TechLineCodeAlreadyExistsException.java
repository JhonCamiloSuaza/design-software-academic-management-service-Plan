package co.sena.academic.domain.exception;

/**
 * Thrown when "code" already exists in academic_management.tech_line
 * (uq_tech_line_code constraint). The REST adapter translates this to
 * 409 CONFLICT with error code ACAD_TECH_LINE_CODE_ALREADY_EXISTS
 * (additive extension proposed to error-codes.yaml — see the HU's DoR).
 */
public class TechLineCodeAlreadyExistsException extends RuntimeException {

    private final String code;

    public TechLineCodeAlreadyExistsException(String code) {
        super("A technical line with code=" + code + " already exists");
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}