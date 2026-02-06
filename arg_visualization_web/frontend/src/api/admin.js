import request from '@/utils/request';

// 获取所有用户列表（不分页）
export function getAllUsers() {
  return request({
    url: '/admin/users',
    method: 'get'
  });
}

// 分页获取用户列表（支持关键字）
export function getUsersPage(params) {
  return request({
    url: '/admin/users/page',
    method: 'get',
    params
  });
}

// 删除用户
export function deleteUser(userId) {
  return request({
    url: `/admin/users/${userId}`,
    method: 'delete'
  });
}

// 封禁/解封用户
export function banUser(userId, ban) {
  return request({
    url: `/admin/users/${userId}/ban`,
    method: 'post',
    params: { ban }
  });
}

// 获取所有文件列表（不分页）
export function getAllFiles() {
  return request({
    url: '/admin/files',
    method: 'get'
  });
}

// 分页获取文件列表（支持用户/文件关键字）
export function getFilesPage(params) {
  return request({
    url: '/admin/files/page',
    method: 'get',
    params
  });
}

// 删除文件（管理员）
export function deleteFile(fileId) {
  return request({
    url: `/admin/files/${fileId}`,
    method: 'delete'
  });
}

// 获取系统统计信息
export function getStatistics() {
  return request({
    url: '/admin/statistics',
    method: 'get'
  });
}

// 搜索用户（根据用户名或用户ID）
export function searchUsers(keyword) {
  return request({
    url: '/admin/users/search',
    method: 'get',
    params: { keyword }
  });
}

// 搜索文件（根据用户信息和文件信息）
export function searchFiles(userKeyword, fileKeyword) {
  const params = {};
  if (userKeyword) {
    params.userKeyword = userKeyword;
  }
  if (fileKeyword) {
    params.fileKeyword = fileKeyword;
  }
  return request({
    url: '/admin/files/search',
    method: 'get',
    params
  });
}

