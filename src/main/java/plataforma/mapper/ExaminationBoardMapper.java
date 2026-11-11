package plataforma.mapper;

import org.springframework.stereotype.Component;
import plataforma.dto.ExaminationBoardRequest;
import plataforma.dto.ExaminationBoardResponse;
import plataforma.model.ExaminationBoard;

@Component
public class ExaminationBoardMapper {

    public ExaminationBoardResponse toResponse(ExaminationBoard board) {
        return new ExaminationBoardResponse(
                board.getId(),
                board.getProject().getId(),
                board.getScheduledAt(),
                board.getEndsAt(),
                board.getLocation(),
                board.getStatus(),
                board.getFinalGrade(),
                board.getCreatedAt());
    }

    /** Copia os campos simples. O projeto é resolvido no service. */
    public void copyToEntity(ExaminationBoardRequest request, ExaminationBoard board) {
        board.setScheduledAt(request.scheduledAt());
        board.setEndsAt(request.endsAt());
        board.setLocation(request.location());
        board.setStatus(request.status());
    }
}
