package plataforma.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import plataforma.model.Notice;

public interface NoticeRepository extends JpaRepository<Notice, UUID> {
}
