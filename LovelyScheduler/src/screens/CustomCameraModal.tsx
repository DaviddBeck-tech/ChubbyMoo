import React, { useState, useRef } from 'react';
import {
  StyleSheet,
  Text,
  View,
  TouchableOpacity,
  Modal,
  ActivityIndicator,
  Dimensions,
  Platform,
} from 'react-native';
import { CameraView, useCameraPermissions } from 'expo-camera';
import * as ImageManipulator from 'expo-image-manipulator';
import { PALETTE, ROUNDED, SPACING, SHADOWS, TYPOGRAPHY } from '../theme/theme';

interface CustomCameraModalProps {
  visible: boolean;
  onClose: () => void;
  onCapture: (imageUri: string) => void;
  taskTitle: string;
}

const { width: SCREEN_WIDTH } = Dimensions.get('window');
const CAMERA_SIZE = SCREEN_WIDTH * 0.78; // Perfect square sizing for maximum cute layout

export default function CustomCameraModal({
  visible,
  onClose,
  onCapture,
  taskTitle,
}: CustomCameraModalProps) {
  const [permission, requestPermission] = useCameraPermissions();
  const [facing, setFacing] = useState<'back' | 'front'>('front'); // Default to cute selfie mode!
  const [isCapturing, setIsCapturing] = useState(false);
  const cameraRef = useRef<any>(null);

  // Toggle camera direction
  const toggleFacing = () => {
    setFacing((current) => (current === 'back' ? 'front' : 'back'));
  };

  // Capture photo and process with image manipulator (Square cropping 1:1, nén 0.7)
  const handleCapture = async () => {
    if (!cameraRef.current || isCapturing) return;

    try {
      setIsCapturing(true);
      const photo = await cameraRef.current.takePictureAsync({
        quality: 0.8,
        skipProcessing: false,
      });

      if (!photo || !photo.uri) {
        throw new Error('Camera failed to capture photo');
      }

      // Process image: Crop to 1:1 square centered slice, then resize to 600x600 for performance
      const size = Math.min(photo.width, photo.height);
      const originX = (photo.width - size) / 2;
      const originY = (photo.height - size) / 2;

      const manipulatedImage = await ImageManipulator.manipulateAsync(
        photo.uri,
        [
          {
            crop: {
              originX,
              originY,
              width: size,
              height: size,
            },
          },
          {
            resize: {
              width: 600,
              height: 600,
            },
          },
        ],
        { compress: 0.7, format: ImageManipulator.SaveFormat.JPEG }
      );

      onCapture(manipulatedImage.uri);
    } catch (error) {
      console.error('Error during photo capture and manipulation:', error);
    } finally {
      setIsCapturing(false);
    }
  };

  // If permission has not loaded yet
  if (!permission) {
    return (
      <Modal visible={visible} transparent animationType="fade">
        <View style={styles.loadingWrapper}>
          <ActivityIndicator size="large" color={PALETTE.primaryLavender} />
        </View>
      </Modal>
    );
  }

  // Cute interactive Permission Modal Screen if not granted
  if (!permission.granted) {
    return (
      <Modal visible={visible} transparent animationType="fade">
        <View style={styles.backdropContainer}>
          <View style={styles.permissionCard}>
            <Text style={styles.permissionEmoji}>📸✨🧚‍♀️</Text>
            <Text style={styles.permissionTitle}>Cấp quyền Camera nè!</Text>
            <Text style={styles.permissionText}>
              Ứng dụng cần quyền sử dụng camera của cậu để chụp ảnh check-in thật xinh đẹp lúc hoàn thành công việc đó! Cho phép mình kết nối camera nhé? 💕
            </Text>
            
            <TouchableOpacity style={styles.permissionBtn} onPress={requestPermission}>
              <Text style={styles.permissionBtnText}>Đồng Ý Nha! 🥰</Text>
            </TouchableOpacity>
            
            <TouchableOpacity style={styles.cancelLink} onPress={onClose}>
              <Text style={styles.cancelLinkText}>Để sau nhé</Text>
            </TouchableOpacity>
          </View>
        </View>
      </Modal>
    );
  }

  return (
    <Modal visible={visible} transparent animationType="slide" onRequestClose={onClose}>
      <View style={styles.backdropContainer}>
        {/* Main interactive cute frame modal */}
        <View style={[styles.modalCard, SHADOWS.lg]}>
          {/* Header */}
          <View style={styles.headerRow}>
            <Text style={styles.headerLabel}>✨ CHỤP ẢNH CHECK-IN ✨</Text>
            <Text style={styles.taskTitleLabel} numberOfLines={1}>
              {taskTitle}
            </Text>
          </View>

          {/* Camera Frame Preview Container */}
          <View style={styles.cameraBoxOutline}>
            <View style={styles.cameraFrame}>
              <CameraView ref={cameraRef} style={styles.cameraView} facing={facing}>
                {isCapturing && (
                  <View style={styles.clippingOverlay}>
                    <ActivityIndicator size="large" color="#FFFFFF" strokeCap="round" />
                    <Text style={styles.processingText}>Đang nén ảnh xinh xắn...</Text>
                  </View>
                )}
              </CameraView>
            </View>
          </View>

          {/* Camera action row */}
          <View style={styles.actionControlsRow}>
            {/* Camera Swap Front/Back */}
            <TouchableOpacity style={styles.cuteCircleBtn} onPress={toggleFacing} disabled={isCapturing}>
              <Text style={styles.cuteBtnEmoji}>🤳</Text>
              <Text style={styles.cuteBtnSubText}>Lật</Text>
            </TouchableOpacity>

            {/* Shutter Button centered */}
            <TouchableOpacity
              style={[styles.bigShutterOuter, isCapturing && styles.disabledShutter]}
              onPress={handleCapture}
              disabled={isCapturing}
            >
              <View style={styles.bigShutterInner} />
            </TouchableOpacity>

            {/* Cancel trigger */}
            <TouchableOpacity style={styles.cuteCircleBtn} onPress={onClose} disabled={isCapturing}>
              <Text style={styles.cuteBtnEmoji}>❌</Text>
              <Text style={styles.cuteBtnSubText}>Hủy</Text>
            </TouchableOpacity>
          </View>
        </View>
      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  loadingWrapper: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: 'rgba(0, 0, 0, 0.4)',
  },
  backdropContainer: {
    flex: 1,
    backgroundColor: 'rgba(28, 25, 23, 0.82)', // Dark aesthetic to make camera popup focus
    justifyContent: 'center',
    alignItems: 'center',
    padding: SPACING.lg,
  },
  permissionCard: {
    width: '88%',
    backgroundColor: '#FFFFFF',
    borderRadius: ROUNDED.xxl,
    padding: SPACING.xxl,
    alignItems: 'center',
    borderWidth: 4,
    borderColor: PALETTE.primaryLavender,
    ...SHADOWS.lg,
  },
  permissionEmoji: {
    fontSize: 48,
    marginBottom: SPACING.md,
  },
  permissionTitle: {
    fontSize: 20,
    fontWeight: 'bold',
    color: PALETTE.textCharcoal,
    marginBottom: SPACING.md,
    textAlign: 'center',
  },
  permissionText: {
    fontSize: 14,
    color: PALETTE.textGray,
    textAlign: 'center',
    lineHeight: 21,
    marginBottom: SPACING.xl,
  },
  permissionBtn: {
    backgroundColor: PALETTE.primaryLavender,
    width: '100%',
    paddingVertical: SPACING.md,
    borderRadius: ROUNDED.xl,
    alignItems: 'center',
    borderWidth: 2,
    borderColor: '#D0A0D5',
    ...SHADOWS.sm,
  },
  permissionBtnText: {
    color: PALETTE.textCharcoal,
    fontWeight: 'bold',
    fontSize: 16,
  },
  cancelLink: {
    marginTop: SPACING.lg,
    padding: SPACING.xs,
  },
  cancelLinkText: {
    color: PALETTE.textLightGray,
    fontSize: 14,
    textDecorationLine: 'underline',
  },
  modalCard: {
    width: '92%',
    backgroundColor: PALETTE.backgroundCream,
    borderRadius: 32, // Huge corner styling matching cute guidelines
    padding: SPACING.xl,
    alignItems: 'center',
    borderWidth: 6,
    borderColor: PALETTE.secondaryPink,
  },
  headerRow: {
    width: '100%',
    alignItems: 'center',
    marginBottom: SPACING.md,
  },
  headerLabel: {
    fontSize: 13,
    fontWeight: 'bold',
    color: PALETTE.focusPink,
    letterSpacing: 1.5,
    marginBottom: 4,
  },
  taskTitleLabel: {
    fontSize: 16,
    fontWeight: '600',
    color: PALETTE.textCharcoal,
    textAlign: 'center',
    paddingHorizontal: SPACING.md,
  },
  cameraBoxOutline: {
    width: CAMERA_SIZE + 12,
    height: CAMERA_SIZE + 12,
    backgroundColor: '#FFFFFF',
    borderRadius: 30, // Nested corner border standard
    justifyContent: 'center',
    alignItems: 'center',
    borderWidth: 3,
    borderColor: 'rgba(0,0,0,0.06)',
    marginBottom: SPACING.xxl,
    ...SHADOWS.sm,
  },
  cameraFrame: {
    width: CAMERA_SIZE,
    height: CAMERA_SIZE,
    borderRadius: ROUNDED.xxl, // 24px/dp corners Locket layout
    overflow: 'hidden',
    backgroundColor: '#1E1E1E',
  },
  cameraView: {
    flex: 1,
    width: '100%',
    height: '100%',
  },
  clippingOverlay: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: 'rgba(0,0,0,0.65)',
    justifyContent: 'center',
    alignItems: 'center',
    padding: SPACING.lg,
  },
  processingText: {
    color: '#FFFFFF',
    marginTop: SPACING.md,
    fontWeight: '500',
    fontSize: 14,
  },
  actionControlsRow: {
    flexDirection: 'row',
    width: '100%',
    justifyContent: 'space-around',
    alignItems: 'center',
    paddingHorizontal: SPACING.xs,
  },
  cuteCircleBtn: {
    width: 60,
    height: 60,
    borderRadius: 30,
    backgroundColor: '#FFFFFF',
    justifyContent: 'center',
    alignItems: 'center',
    borderWidth: 2,
    borderColor: PALETTE.secondaryPink,
    ...SHADOWS.sm,
  },
  cuteBtnEmoji: {
    fontSize: 22,
  },
  cuteBtnSubText: {
    fontSize: 9,
    fontWeight: '700',
    color: PALETTE.textGray,
    marginTop: -2,
  },
  bigShutterOuter: {
    width: 80,
    height: 80,
    borderRadius: 40,
    borderWidth: 5,
    borderColor: PALETTE.primaryLavender,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#FFFFFF',
    ...SHADOWS.md,
  },
  bigShutterInner: {
    width: 60,
    height: 60,
    borderRadius: 30,
    backgroundColor: PALETTE.focusPink,
  },
  disabledShutter: {
    opacity: 0.5,
  },
});
