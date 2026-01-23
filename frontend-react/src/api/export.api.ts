import { config } from '../config/env';
import { auth } from '../lib/auth';

export const exportApi = {
  json: () => {
    // This would be imported from api-client but needs auth headers
    return fetch(`${config.apiUrl}/export/json`, {
      headers: auth.getHeaders(),
    }).then(res => res.json());
  },
  importJson: (data: any) => {
    return fetch(`${config.apiUrl}/export/import/json`, {
      method: 'POST',
      headers: auth.getHeaders(),
      body: JSON.stringify(data),
    }).then(res => res.json());
  },
  getPdfUrl: () => `${config.apiUrl}/export/pdf?token=${auth.getToken()}`,
  getExcelUrl: () => `${config.apiUrl}/export/excel?token=${auth.getToken()}`,
};
