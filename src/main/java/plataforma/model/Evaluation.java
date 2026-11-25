package plataforma.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;

/** Notas de um avaliador da banca, por critério da rubrica (0 a 10). */
@Entity
@Table(schema = "academic_pi", name = "evaluation")
public class Evaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "board_id", nullable = false)
    private ExaminationBoard board;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evaluator_id", nullable = false)
    private UserAccount evaluator;

    @Column(name = "technical_grade", nullable = false, precision = 4, scale = 2)
    private BigDecimal technicalGrade;

    @Column(name = "presentation_grade", nullable = false, precision = 4, scale = 2)
    private BigDecimal presentationGrade;

    @Column(name = "artifact_grade", nullable = false, precision = 4, scale = 2)
    private BigDecimal artifactGrade;

    /** TODO BE-015: calcular pela rubrica de forma determinística e auditável. */
    @Column(name = "final_grade", precision = 4, scale = 2)
    private BigDecimal finalGrade;

    @Column(columnDefinition = "text")
    private String feedback;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public UUID getId() {
        return id;
    }

    public ExaminationBoard getBoard() {
        return board;
    }

    public void setBoard(ExaminationBoard board) {
        this.board = board;
    }

    public UserAccount getEvaluator() {
        return evaluator;
    }

    public void setEvaluator(UserAccount evaluator) {
        this.evaluator = evaluator;
    }

    public BigDecimal getTechnicalGrade() {
        return technicalGrade;
    }

    public void setTechnicalGrade(BigDecimal technicalGrade) {
        this.technicalGrade = technicalGrade;
    }

    public BigDecimal getPresentationGrade() {
        return presentationGrade;
    }

    public void setPresentationGrade(BigDecimal presentationGrade) {
        this.presentationGrade = presentationGrade;
    }

    public BigDecimal getArtifactGrade() {
        return artifactGrade;
    }

    public void setArtifactGrade(BigDecimal artifactGrade) {
        this.artifactGrade = artifactGrade;
    }

    public BigDecimal getFinalGrade() {
        return finalGrade;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
