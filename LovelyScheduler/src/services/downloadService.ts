import * as FileSystem from 'expo-file-system';
import * as MediaLibrary from 'expo-media-library';
import { Alert } from 'react-native';

/**
 * Download check-in image to the user's phone media gallery.
 * Handles permission requests and creates/saves inside a custom 'LovelyScheduler' album.
 * 
 * @param imageUri Local file URI of the captured proof image (e.g. 'file://...')
 * @returns boolean representing success state
 */
export async function downloadProofImage(imageUri: string): Promise<boolean> {
  if (!imageUri) {
    Alert.alert('Úi kìa 😳', 'Không tìm thấy hình ảnh nào để tải xuống cậu ơi.');
    return false;
  }

  try {
    // 1. Xin quyền truy cập Thư viện ảnh (Media Library permission)
    const { status } = await MediaLibrary.requestPermissionsAsync(true);
    
    if (status !== 'granted') {
      Alert.alert(
        'Quyền truy cập bị từ chối 🦄',
        'Cậu ơi, ứng dụng cần quyền truy cập thư viện ảnh để có thể lưu tấm hình check-in này về máy nha. Cậu cho phép trong phần cài đặt điện thoại nhé! 💕'
      );
      return false;
    }

    // 2. Kiểm tra xem file có thực sự tồn tại trong FileSystem không
    const fileInfo = await FileSystem.getInfoAsync(imageUri);
    if (!fileInfo.exists) {
      Alert.alert('Úi, lỗi xảy ra 😢', 'Tệp ảnh check-in tam thời không còn tồn tại.');
      return false;
    }

    // 3. Tạo Asset từ file ảnh tạm thời
    const asset = await MediaLibrary.createAssetAsync(imageUri);
    
    // 4. Tạo album hoặc thêm vào album 'LovelyScheduler' để người dùng dễ tra cứu
    const albumName = 'LovelyScheduler';
    const existingAlbum = await MediaLibrary.getAlbumAsync(albumName);

    if (existingAlbum === null) {
      await MediaLibrary.createAlbumAsync(albumName, asset, false);
    } else {
      await MediaLibrary.addAssetsToAlbumAsync([asset], existingAlbum, false);
    }

    Alert.alert(
      'Tải ảnh thành công! 🎉',
      'Tấm hình check-in hoàn thành công việc của cậu đã được lưu thành công vào thư viện ảnh rồi đó nha! Chúc mừng cậu nhé! 🥰📸'
    );
    return true;
  } catch (error) {
    console.error('Error during photo downloading flow:', error);
    Alert.alert(
      'Trục trặc một tẹo 😢',
      'Có lỗi xảy ra khi lưu ảnh. Cậu kiểm tra lại dung lượng máy hoặc thử lại sau nhé!'
    );
    return false;
  }
}
