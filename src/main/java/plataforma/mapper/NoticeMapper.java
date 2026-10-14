package plataforma.mapper;

import org.springframework.stereotype.Component;
import plataforma.dto.NoticeRequest;
import plataforma.dto.NoticeResponse;
import plataforma.model.Notice;

@Component
public class NoticeMapper {

    public NoticeResponse toResponse(Notice notice) {
        return new NoticeResponse(
                notice.getId(),
                notice.getTitle(),
                notice.getDescription(),
                notice.getStartsOn(),
                notice.getEndsOn(),
                notice.getTotalHours(),
                notice.getAdvisor().getId(),
                notice.isActive(),
                notice.getCreatedAt());
    }

    /** Copia os campos simples. O orientador é resolvido no service. */
    public void copyToEntity(NoticeRequest request, Notice notice) {
        notice.setTitle(request.title());
        notice.setDescription(request.description());
        notice.setStartsOn(request.startsOn());
        notice.setEndsOn(request.endsOn());
        notice.setTotalHours(request.totalHours());
        notice.setActive(request.active());
    }
}
