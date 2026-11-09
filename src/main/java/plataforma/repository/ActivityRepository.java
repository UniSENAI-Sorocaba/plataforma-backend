package plataforma.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import plataforma.model.Activity;

public interface ActivityRepository extends JpaRepository<Activity, UUID> {

    /** Soma das horas aprovadas das atividades deferidas de uma inscrição (0 quando não há nenhuma). */
    @Query("""
            select coalesce(sum(a.hoursApproved), 0) from Activity a
            where a.enrollment.id = :enrollmentId and a.status = plataforma.model.ReviewStatus.APPROVED
            """)
    long sumApprovedHours(@Param("enrollmentId") UUID enrollmentId);
}
