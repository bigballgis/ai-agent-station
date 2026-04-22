import request from '@/utils/request';

export interface Approval {
  id: number;
  agentId: number;
  tenantId: number;
  agentVersionId: number;
  submitterId: number;
  approverId?: number;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  remark?: string;
  approvalRemark?: string;
  submittedAt: string;
  approvedAt?: string;
  createdAt: string;
  updatedAt: string;
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

export const approvalApi = {
  getApprovals: (page = 0, size = 10) => {
    return request.get<ApiResponse<PageResult<Approval>>>('/approvals', {
      params: { page, size }
    });
  },

  getPendingApprovals: (page = 0, size = 10) => {
    return request.get<ApiResponse<PageResult<Approval>>>('/approvals/pending', {
      params: { page, size }
    });
  },

  getApprovalById: (id: number) => {
    return request.get<ApiResponse<Approval>>(`/approvals/${id}`);
  },

  getApprovalsByAgentId: (agentId: number) => {
    return request.get<ApiResponse<Approval[]>>(`/approvals/agent/${agentId}`);
  },

  submitForApproval: (agentId: number, versionId: number, remark?: string) => {
    return request.post<ApiResponse<Approval>>('/approvals/submit', {
      agentId,
      versionId,
      remark
    });
  },

  approve: (id: number, approvalRemark?: string) => {
    return request.post<ApiResponse<Approval>>(`/approvals/${id}/approve`, {
      approvalRemark
    });
  },

  reject: (id: number, approvalRemark?: string) => {
    return request.post<ApiResponse<Approval>>(`/approvals/${id}/reject`, {
      approvalRemark
    });
  }
};
