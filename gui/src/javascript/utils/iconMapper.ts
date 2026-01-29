import { cilFolderOpen, cilFile, cilImage, cilMusicNote, cilVideo, cilCode, cilDescription } from '@coreui/icons'
import Node from '../api/entities/Node'

export const getIcon = (node: Node) => {
  if (node.isFolder) return cilFolderOpen;
  const mimetype = node.mimetype || '';
  if (mimetype.startsWith('image/')) return cilImage;
  if (mimetype.startsWith('audio/')) return cilMusicNote;
  if (mimetype.startsWith('video/')) return cilVideo;
  if (mimetype === 'application/pdf') return cilDescription;
  if (mimetype === 'text/plain') return cilFile;
  if (mimetype === 'text/html' || mimetype === 'application/json') return cilCode;
  if (mimetype === 'application/gzip' || mimetype === 'application/zip') return cilFile;
  if (mimetype === 'application/msword' || mimetype === 'application/vnd.openxmlformats-officedocument.wordprocessingml.document') return cilDescription;
  if (mimetype === 'application/vnd.ms-excel' || mimetype === 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet') return cilDescription;
  if (mimetype === 'application/vnd.ms-powerpoint' || mimetype === 'application/vnd.openxmlformats-officedocument.presentationml.presentation') return cilDescription;
  return cilFile;
};
