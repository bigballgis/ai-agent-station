import request from '@/utils/request';
import type { ApiResponse, PageResult } from '@/types/common';

export interface Deployment {
  id: number;
  agentId: number;
  tenantId: number;
  agentVersionId: number;
  approverId?: number;
  deployerId: number;
  status: 'PENDING' | 'DEPLOYING' | 'SUCCESS' | 'FAILED' | 'ROLLED_BACK';
  version: string;
  isCanary: boolean;
  canaryPercentage: number;
  rollbackFromId?: number;
  deployedAt?: string;
  rollbackAt?: string;
  remark?: string;
  createdAt: string;
  updatedAt: string;
}

export interface VersionComparison {
  version1: unknown;
  version2: unknown;
  configDiff: {
    added: Record<string, unknown>;
    removed: Record<string, unknown>;
    modified: Record<string, { old: unknown; new: unknown }>;
  };
}

export const deploymentApi = {
  getDeployments: (page = 0, size = 10) => {
    return request.get<ApiResponse<PageResult<Deployment>>>('/v1/deployments', {
      params: { page, size }
    });
  },

  getDeploymentsByAgentId: (agentId: number) => {
    return request.get<ApiResponse<Deployment[]>>(`/v1/deployments/agent/${agentId}`);
  },

  getDeploymentById: (id: number) => {
    return request.get<ApiResponse<Deployment>>(`/v1/deployments/${id}`);
  },

  deploy: (agentId: number, versionId: number, isCanary = false, canaryPercentage = 100, remark?: string) => {
    return request.post<ApiResponse<Deployment>>('/v1/deployments/deploy', {
      agentId,
      versionId,
      isCanary,
      canaryPercentage,
      remark
    });
  },

  rollback: (id: number) => {
    return request.post<ApiResponse<Deployment>>(`/v1/deployments/${id}/rollback`);
  },

  compareVersions: (versionId1: number, versionId2: number) => {
    return request.get<ApiResponse<VersionComparison>>('/v1/deployments/compare', {
      params: { versionId1, versionId2 }
    });
  }
};
