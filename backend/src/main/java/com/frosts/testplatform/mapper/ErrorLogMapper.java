package com.frosts.testplatform.mapper;

import com.frosts.testplatform.dto.errorreport.ErrorLogResponse;
import com.frosts.testplatform.dto.errorreport.ErrorOverviewResponse;
import com.frosts.testplatform.entity.ErrorLog;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.format.DateTimeFormatter;

@Mapper(componentModel = "spring")
public interface ErrorLogMapper {

    ErrorLogResponse toResponse(ErrorLog entity);

    @Mapping(target = "errorMessage", expression = "java(truncate(entity.getErrorMessage(), 80))")
    @Mapping(target = "createdAt", expression = "java(formatCreatedAt(entity))")
    ErrorOverviewResponse.RecentError toRecentError(ErrorLog entity);

    default String formatCreatedAt(ErrorLog entity) {
        if (entity.getCreatedAt() == null) return "";
        return entity.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    default String truncate(String value, int max) {
        if (value == null) return null;
        return value.length() > max ? value.substring(0, max) : value;
    }
}
