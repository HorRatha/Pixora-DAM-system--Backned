package com.dam.digitalassetmanagement.search;

import com.dam.digitalassetmanagement.enums.AssetStatus;
import com.dam.digitalassetmanagement.enums.AssetType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;

/**
 * AssetDocument - Elasticsearch document representation of an Asset
 *
 * IMPORTANT: Date Handling
 * - LocalDateTime fields are stored in Elasticsearch as ISO-8601 strings with full precision
 * - The @Field annotation with type=FieldType.Date and pattern supports multiple formats on read
 * - The @JsonFormat annotation ensures consistent serialization format on write
 * - Both annotations work together: pattern handles inbound flexibility, JsonFormat handles outbound consistency
 *
 * Pattern explanation: "uuuu-MM-dd'T'HH:mm:ss.SSSSSS||uuuu-MM-dd'T'HH:mm:ss.SSS||uuuu-MM-dd'T'HH:mm:ss||uuuu-MM-dd"
 * This allows Elasticsearch to parse dates in multiple formats (with microseconds, milliseconds, seconds-only, or date-only)
 *
 * JsonFormat ensures all dates are written in format: "2025-12-17T19:35:07.198277"
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "assets")
public class AssetDocument {

    @Id
    private Long assetId;

    @Field(type = FieldType.Text)
    private String title;

    @Field(type = FieldType.Text)
    private String description;

    @Field(type = FieldType.Keyword)
    private AssetType type;

    @Field(type = FieldType.Keyword)
    private AssetStatus status;

    @Field(type = FieldType.Keyword)
    private String fileUrl;

    @Field(type = FieldType.Keyword)
    private String thumbnailUrl;

    @Field(type = FieldType.Long)
    private Long userId;

    @Field(type = FieldType.Keyword)
    private String username;

    @Field(type = FieldType.Integer)
    private Integer version;

    @Field(type = FieldType.Boolean)
    private Boolean isActive;

    /**
     * Created timestamp with full datetime precision
     * Stored in Elasticsearch as: "2025-12-17T19:35:07.198277"
     */
    @Field(type = FieldType.Date, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSSSSS||uuuu-MM-dd'T'HH:mm:ss.SSS||uuuu-MM-dd'T'HH:mm:ss||uuuu-MM-dd")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    private LocalDateTime createdAt;

    /**
     * Last updated timestamp with full datetime precision
     * Stored in Elasticsearch as: "2025-12-18T10:20:30.123456"
     */
    @Field(type = FieldType.Date, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSSSSS||uuuu-MM-dd'T'HH:mm:ss.SSS||uuuu-MM-dd'T'HH:mm:ss||uuuu-MM-dd")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    private LocalDateTime updatedAt;

    @Field(type = FieldType.Text)
    private String tags;

    @Field(type = FieldType.Keyword)
    private String fileExtension;

    @Field(type = FieldType.Long)
    private Long fileSize;
}