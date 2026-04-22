package com.aiagent.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "dict_types")
public class DictType extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dict_name", nullable = false, length = 100)
    private String dictName;

    @Column(name = "dict_type", nullable = false, unique = true, length = 100)
    private String dictType;

    @Column(name = "status", length = 10)
    private String status = "active";

    @Column(name = "remark", length = 500)
    private String remark;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;
}
