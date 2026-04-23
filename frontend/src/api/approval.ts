import request from '@/utils/request';
import type { ApiResponse, PageResult } from '@/types/common';

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

export const approvalApi = {
  getApprovals: (page = 0, size = 10) => {
    return request.get<ApiResponse<PageResult<Approval>>>('/v1/approvals', {
      params: { page, size }
    });
  },

  getPendingApprovals: (page = 0, size = 10) => {
    return request.get<ApiResponse<PageResult<Approval>>>('/v1/approvals/pending', {
      params: { page, size }
    });
  },

  getApprovalById: (id: number) => {
    return request.get<ApiResponse<Approval>>(`/v1/approvals/${id}`);
  },

  getApprovalsByAgentId: (agentId: number) => {
    return request.get<ApiResponse<Approval[]>>(`/v1/approvals/agent/${agentId}`);
  },

  submitForApproval: (agentId: number, versionId: number, remark?: string) => {
    return request.post<ApiResponse<Approval>>('/v1/approvals/submit', {
      agentId,
      versionId,
      remark
    });
  },

  approve: (id: number, approvalRemark?: string) => {
    return request.post<ApiResponse<Approval>>(`/v1/approvals/${id}/approve`, {
      approvalRemark
    });
  },

  reject: (id: number, approvalRemark?: string) => {
    return request.post<ApiResponse<Approval>>(`/v1/approvals/${id}/reject`, {
      approvalRemark
    });
  }
};
