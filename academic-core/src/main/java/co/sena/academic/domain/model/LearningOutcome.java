package co.sena.academic.domain.model;

import java.time.Instant;
import java.util.UUID;

public final class LearningOutcome {
    private final UUID id; private final UUID competencyId; private String code; private String description;
    private boolean active; private int rowVersion; private final Instant createdAt; private final UUID createdBy;
    private Instant updatedAt; private UUID updatedBy; private Instant deletedAt; private UUID deletedBy;
    private LearningOutcome(UUID id, UUID competencyId, String code, String description, boolean active, int rowVersion,
                            Instant createdAt, UUID createdBy, Instant updatedAt, UUID updatedBy, Instant deletedAt, UUID deletedBy) {
        this.id=req(id,"id"); this.competencyId=req(competencyId,"competencyId"); this.code=code(code); this.description=description(description);
        this.active=active; this.rowVersion=rowVersion; this.createdAt=req(createdAt,"createdAt"); this.createdBy=req(createdBy,"createdBy");
        this.updatedAt=req(updatedAt,"updatedAt"); this.updatedBy=req(updatedBy,"updatedBy"); this.deletedAt=deletedAt; this.deletedBy=deletedBy;
    }
    public static LearningOutcome create(UUID competencyId, String code, String description, Boolean active, UUID actor) {
        Instant now=Instant.now(); return new LearningOutcome(UUID.randomUUID(), competencyId, code, description, active==null||active,0,now,actor,now,actor,null,null);
    }
    public static LearningOutcome reconstitute(UUID id, UUID competencyId, String code, String description, boolean active, int rowVersion, Instant createdAt, UUID createdBy, Instant updatedAt, UUID updatedBy, Instant deletedAt, UUID deletedBy) {
        return new LearningOutcome(id,competencyId,code,description,active,rowVersion,createdAt,createdBy,updatedAt,updatedBy,deletedAt,deletedBy);
    }
    public void update(String code, String description, Boolean active, UUID actor) { ensureMutable(); if(code!=null)this.code=code(code); if(description!=null)this.description=description(description); if(Boolean.FALSE.equals(active)){deactivate(actor);return;} this.updatedAt=Instant.now();this.updatedBy=req(actor,"actorId"); }
    public void deactivate(UUID actor){if(!active||deletedAt!=null)return; Instant now=Instant.now(); this.active=false;this.deletedAt=now;this.deletedBy=req(actor,"actorId");this.updatedAt=now;this.updatedBy=actor;}
    private void ensureMutable(){if(!active||deletedAt!=null)throw new IllegalStateException("A deactivated learning outcome cannot be updated or reactivated");}
    private static String code(String v){if(v==null||v.isBlank())throw new IllegalArgumentException("code is required");if(v.length()>20)throw new IllegalArgumentException("code must not exceed 20 characters");return v;}
    private static String description(String v){if(v==null||v.isBlank())throw new IllegalArgumentException("description is required");return v;}
    private static <T>T req(T v,String n){if(v==null)throw new IllegalArgumentException(n+" is required");return v;}
    public UUID getId(){return id;} public UUID getCompetencyId(){return competencyId;} public String getCode(){return code;} public String getDescription(){return description;} public boolean isActive(){return active;} public int getRowVersion(){return rowVersion;} public Instant getCreatedAt(){return createdAt;} public UUID getCreatedBy(){return createdBy;} public Instant getUpdatedAt(){return updatedAt;} public UUID getUpdatedBy(){return updatedBy;} public Instant getDeletedAt(){return deletedAt;} public UUID getDeletedBy(){return deletedBy;}
}
