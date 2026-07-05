import axios from 'axios';

// Connect to Spring Boot on port 8080
const API_BASE = 'http://localhost:8080/api';

const api = axios.create({
    baseURL: API_BASE,
    headers: {
        'Content-Type': 'application/json',
    },
});

/**
 * REST Endpoint clients. Mapping backend controllers.
 */
export const authService = {
    register: async (username, password, role, wardArea) => {
        const response = await api.post('/auth/register', { username, password, role, wardArea });
        return response.data;
    },
    login: async (username, password) => {
        const response = await api.post('/auth/login', { username, password });
        return response.data;
    },
};

export const complaintService = {
    // Post a new citizen complaint
    submit: async (originalText, wardArea, citizenId) => {
        const response = await api.post('/complaints', { originalText, wardArea, citizenId });
        return response.data;
    },
    // Get all active master complaints in area
    getByWard: async (wardArea) => {
        const response = await api.get(`/complaints?wardArea=${encodeURIComponent(wardArea)}`);
        return response.data;
    },
    // Search complaints by text matching
    search: async (wardArea, query) => {
        const response = await api.get(`/complaints/search?wardArea=${encodeURIComponent(wardArea)}&query=${encodeURIComponent(query)}`);
        return response.data;
    },
    // Cast upvote
    upvote: async (complaintId, userId) => {
        const response = await api.post(`/complaints/${complaintId}/upvote?userId=${userId}`);
        return response.data;
    },
};

export const mpService = {
    // Fetch dashboard stats (counters, category maps, dynamic AI briefings)
    getStats: async (wardArea) => {
        const response = await api.get(`/mp/dashboard/stats?wardArea=${encodeURIComponent(wardArea)}`);
        return response.data;
    },
    // Update complaint status (PENDING -> IN_PROGRESS -> RESOLVED)
    updateStatus: async (complaintId, status) => {
        const response = await api.put(`/mp/complaints/${complaintId}/status?status=${status}`);
        return response.data;
    },
};

export default api;
