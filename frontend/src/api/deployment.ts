import request from '@/utils/request';

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
  version1: any;
  version2: any;
  configDiff: {
    added: Record<string, any>;
    removed: Record<string, any>;
    modified: Record<string, { old: any; new: any }>;
  };
}

export interface PageResult<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}

export const deploymentApi = {
  getDeployments: (page = 0, size = 10) => {
    return request.get<ApiResponse<PageResult<Deployment>>>('/deployments', {
      params: { page, size }
    });
  },

  getDeploymentsByAgentId: (agentId: number) => {
    return request.get<ApiResponse<Deployment[]>>(`/deployments/agent/${agentId}`);
  },

  getDeploymentById: (id: number) => {
    return request.get<ApiResponse<Deployment>>(`/deployments/${id}`);
  },

  deploy: (agentId: number, versionId: number, isCanary = false, canaryPercentage = 100, remark?: string) => {
    return request.post<ApiResponse<Deployment>>('/deployments/deploy', {
      agentId,
      versionId,
      isCanary,
      canaryPercentage,
      remark
    });
  },

  rollback: (id: number) => {
    return request.post<ApiResponse<Deployment>>(`/deployments/${id}/rollback`);
  },

  compareVersions: (versionId1: number, versionId2: number) => {
    return request.get<ApiResponse<VersionComparison>>('/deployments/compare', {
      params: { versionId1, versionId2 }
    });
  }
};
