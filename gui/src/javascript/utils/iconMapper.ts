///
/// Copyright (C) 2025 Jerome Blanchard <jayblanc@gmail.com>
///
/// This program is free software: you can redistribute it and/or modify
/// it under the terms of the GNU General Public License as published by
/// the Free Software Foundation, either version 3 of the License, or
/// (at your option) any later version.
///
/// This program is distributed in the hope that it will be useful,
/// but WITHOUT ANY WARRANTY; without even the implied warranty of
/// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
/// GNU General Public License for more details.
///
/// You should have received a copy of the GNU General Public License
/// along with this program.  If not, see <https://www.gnu.org/licenses/>.
///

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
