package plataforma.model;

import jakarta.persistence.Column;
import jakarta.persistence.JoinTable;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.LinkedHashSet;
import java.util.Set;
import org.hibernate.annotations.CreationTimestamp;

/** Banca examinadora de um projeto (uma por projeto). */
// TODO BE-011: impedir conflito de horário e vínculo inválido de avaliadores.
@Entity
@Table(schema = "academic_pi", name = "examination_board")
public class ExaminationBoard {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false, unique = true)
    private Project project;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(schema = "academic_pi", name = "examination_board_evaluator",
            joinColumns = @JoinColumn(name = "board_id"),
            inverseJoinColumns = @JoinColumn(name = "evaluator_id"))
    private Set<UserAccount> evaluators = new LinkedHashSet<>();

    @Column(name = "scheduled_at", nullable = false)
    private Instant scheduledAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    /** Sala ou link da apresentação. */
    @Column(nullable = false)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BoardStatus status = BoardStatus.SCHEDULED;

    /** Nota consolidada da banca, preenchida quando todos os avaliadores enviam suas notas. */
    @Column(name = "final_grade", precision = 4, scale = 2)
    private BigDecimal finalGrade;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public UUID getId() {
        return id;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public Set<UserAccount> getEvaluators() {
        return Set.copyOf(evaluators);
    }

    public void replaceEvaluators(Set<UserAccount> evaluators) {
        this.evaluators.clear();
        this.evaluators.addAll(evaluators);
    }

    public boolean hasEvaluator(UserAccount evaluator) {
        return evaluators.stream().anyMatch(candidate -> candidate.getId().equals(evaluator.getId()));
    }

    public Instant getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(Instant scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public Instant getEndsAt() {
        return endsAt;
    }

    public void setEndsAt(Instant endsAt) {
        this.endsAt = endsAt;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public BoardStatus getStatus() {
        return status;
    }

    public void setStatus(BoardStatus status) {
        this.status = status;
    }

    public BigDecimal getFinalGrade() {
        return finalGrade;
    }

    public void setFinalGrade(BigDecimal finalGrade) {
        this.finalGrade = finalGrade;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
