package com.dam.digitalassetmanagement.search;

import com.dam.digitalassetmanagement.enums.AssetStatus;
import com.dam.digitalassetmanagement.enums.AssetType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "assets")
public class AssetDocument {

    @Id
    private Long assetId;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String title;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String description;

    @Field(type = FieldType.Keyword)
    private AssetType type;

    @Field(type = FieldType.Keyword)
    private AssetStatus status;

    @Field(type = FieldType.Text)
    private String fileUrl;

    @Field(type = FieldType.Text)
    private String thumbnailUrl;

    @Field(type = FieldType.Long)
    private Long userId;

    @Field(type = FieldType.Text)
    private String username;

    @Field(type = FieldType.Integer)
    private Integer version;

    @Field(type = FieldType.Boolean)
    private Boolean isActive;

    @Field(type = FieldType.Date)
    private LocalDateTime createdAt;

    @Field(type = FieldType.Date)
    private LocalDateTime updatedAt;

    // Metadata fields for advanced search
    @Field(type = FieldType.Text)
    private String tags; // Comma-separated tags

    @Field(type = FieldType.Keyword)
    private String fileExtension;

    @Field(type = FieldType.Long)
    private Long fileSize;
}