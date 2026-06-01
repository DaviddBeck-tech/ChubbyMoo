import * as Print from 'expo-print';
import * as Sharing from 'expo-sharing';
import * as FileSystem from 'expo-file-system';
import { Task } from '../types/types';
import { CATEGORIES } from '../store/taskStore';

/**
 * Đọc file từ local URI và chuyển đổi thành chuỗi định dạng Base64
 * Điều này bắt buộc để trình render PDF (Webview) có thể load được tài nguyên local.
 */
export const convertLocalUriToBase64 = async (uri: string): Promise<string> => {
  try {
    if (!uri) return '';
    
    // Nếu uri đã là base64 thì trả về luôn
    if (uri.startsWith('data:')) {
      return uri;
    }
    
    // Đọc tệp tin thành chuỗi base64
    const base64Data = await FileSystem.readAsStringAsync(uri, {
      encoding: FileSystem.EncodingType.Base64,
    });
    
    // Phổ biến là hình ảnh check-in dạng JPG hoặc PNG
    return `data:image/jpeg;base64,${base64Data}`;
  } catch (error) {
    console.error('Lỗi khi convert ảnh local sang base64:', error);
    return '';
  }
};

/**
 * Định dạng ngày thân thiện (dd/MM/yyyy) từ chuỗi 'YYYY-MM-DD'
 */
const formatDateVi = (dateStr: string): string => {
  if (!dateStr) return '';
  const parts = dateStr.split('-');
  if (parts.length === 3) {
    return `${parts[2]}/${parts[1]}/${parts[0]}`;
  }
  return dateStr;
};

/**
 * Xuất danh sách công việc được tích chọn ra một file PDF chuyên nghiệp
 * @param selectedTasks Danh sách task được chọn để đưa vào báo cáo
 */
export const exportWeeklyReportPDF = async (selectedTasks: Task[]): Promise<boolean> => {
  try {
    if (selectedTasks.length === 0) {
      throw new Error('Vui lòng chọn ít nhất một công việc để xuất báo cáo.');
    }

    // 1. Chuyển đổi toàn bộ proofImage của task được chọn sang Base64
    const processedTasks = await Promise.all(
      selectedTasks.map(async (task) => {
        let base64Image = '';
        if (task.status === 'completed' && task.proofImage) {
          base64Image = await convertLocalUriToBase64(task.proofImage);
        }
        return {
          ...task,
          base64Image,
        };
      })
    );

    // 2. Tính toán khoảng ngày báo cáo (ngỏ nhất và lớn nhất trong danh sách các task)
    const dates = selectedTasks.map(t => t.scheduledDate).sort();
    const startDate = dates.length > 0 ? formatDateVi(dates[0]) : formatDateVi(new Date().toISOString().split('T')[0]);
    const endDate = dates.length > 0 ? formatDateVi(dates[dates.length - 1]) : formatDateVi(new Date().toISOString().split('T')[0]);
    const exportDateStr = formatDateVi(new Date().toISOString().split('T')[0]);

    // 3. Xây dựng nội dung HTML động
    let tasksHtml = '';
    processedTasks.forEach((task) => {
      const category = CATEGORIES.find(c => c.id === task.categoryId);
      const categoryName = category ? category.nameVi : 'Khác';
      
      // Chọn nhãn trạng thái và class CSS tương ứng
      let statusLabel = 'Chờ thực hiện';
      let statusBadgeClass = 'badge-pending';
      if (task.status === 'completed') {
        statusLabel = 'Hoàn thành';
        statusBadgeClass = 'badge-completed';
      } else if (task.status === 'moved') {
        statusLabel = 'Đã dời lịch';
        statusBadgeClass = 'badge-moved';
      }

      // Xây dựng phần cột trái chứa hình ảnh
      let leftColHtml = '';
      if (task.base64Image) {
        leftColHtml = `<img src="${task.base64Image}" class="proof-img" alt="Check-in" />`;
      } else {
        // Render box pastel gradient làm placeholder nếu không có ảnh
        const pastelColor = category ? category.color : '#E0E0E0';
        leftColHtml = `
          <div class="placeholder-box" style="background-color: ${pastelColor}33;">
            <div style="font-size: 28px; margin-bottom: 6px;">${category?.emoji || '🌸'}</div>
            <div style="font-weight: bold; font-size: 11px; color: #555;">${categoryName}</div>
            <div style="font-size: 10px; color: #888; margin-top: 4px;">Chưa chụp check-in</div>
          </div>
        `;
      }

      tasksHtml += `
        <div class="task-card">
          <div class="left-col">
            ${leftColHtml}
          </div>
          <div class="right-col">
            <div>
              <div class="task-title">${task.title}</div>
              <div class="task-desc">${task.description || 'Không có mô tả chi tiết cho công việc này.'}</div>
            </div>
            <div class="task-footer">
              <div>
                <span class="badge ${statusBadgeClass}">${statusLabel}</span>
                <span style="font-size: 11px; margin-left: 8px; color: #7f8c8d; font-weight: 500;">
                  #${categoryName}
                </span>
              </div>
              <div class="time-info">
                <strong>Hạn làm:</strong> ${task.reminderTime || 'Cả ngày'}${task.completedAt ? ` | <b>Xong lúc:</b> ${task.completedAt}` : ''}
              </div>
            </div>
          </div>
        </div>
      `;
    });

    const fullHtml = `
      <!DOCTYPE html>
      <html>
      <head>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Weekly Recap Report - Lovely Scheduler</title>
        <style>
          @page {
            size: A4;
            margin: 20mm;
          }
          body {
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
            color: #2c3e50;
            margin: 0;
            padding: 0;
            background-color: #ffffff;
            line-height: 1.5;
          }
          .header {
            border-bottom: 3px double #e0bbe4;
            padding-bottom: 20px;
            margin-bottom: 30px;
            display: flex;
            justify-content: space-between;
            align-items: flex-end;
          }
          .title-area h1 {
            font-size: 20px;
            text-transform: uppercase;
            font-weight: 800;
            letter-spacing: 1.5px;
            color: #34495e;
            margin: 0 0 10px 0;
          }
          .subtitle {
            font-size: 12px;
            color: #7f8c8d;
            margin: 0;
            font-style: italic;
          }
          .meta-info {
            font-size: 12px;
            text-align: right;
            color: #34495e;
          }
          .meta-line {
            margin-bottom: 4px;
          }
          .meta-line b {
            color: #2c3e50;
          }
          .task-list {
            display: flex;
            flex-direction: column;
            gap: 18px;
          }
          .task-card {
            display: flex;
            flex-direction: row;
            border: 1.5px solid #eaeaea;
            border-radius: 12px;
            background-color: #ffffff;
            box-shadow: 0 2px 4px rgba(0,0,0,0.02);
            page-break-inside: avoid;
            overflow: hidden;
            min-height: 130px;
          }
          .left-col {
            width: 32%;
            background-color: #fcfbfb;
            display: flex;
            justify-content: center;
            align-items: stretch;
            border-right: 1.5px solid #eaeaea;
          }
          .proof-img {
            width: 100%;
            height: 100%;
            object-fit: cover;
          }
          .placeholder-box {
            width: 100%;
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            padding: 15px;
            box-sizing: border-box;
            text-align: center;
          }
          .right-col {
            width: 68%;
            padding: 16px;
            display: flex;
            flex-direction: column;
            justify-content: space-between;
          }
          .task-title {
            font-size: 15px;
            font-weight: 700;
            color: #2c3e50;
            margin: 0 0 6px 0;
          }
          .task-desc {
            font-size: 12px;
            color: #7f8c8d;
            margin: 0 0 15px 0;
            text-align: justify;
          }
          .task-footer {
            display: flex;
            justify-content: space-between;
            align-items: center;
            border-top: 1px dashed #ecf0f1;
            padding-top: 10px;
          }
          .badge {
            display: inline-block;
            padding: 4px 8px;
            border-radius: 6px;
            font-weight: 700;
            font-size: 10px;
            letter-spacing: 0.5px;
          }
          .badge-completed {
            background-color: #e2f7f0;
            color: #27ae60;
          }
          .badge-moved {
            background-color: #fef5e7;
            color: #d35400;
          }
          .badge-pending {
            background-color: #f2f4f4;
            color: #7f8c8d;
          }
          .time-info {
            font-size: 11px;
            color: #7f8c8d;
          }
          .time-info b {
            color: #2c3e50;
          }
          .footer-watermark {
            margin-top: 40px;
            text-align: center;
            font-size: 10px;
            color: #bdc3c7;
            border-top: 1px solid #f2f4f4;
            padding-top: 15px;
            page-break-inside: avoid;
          }
        </style>
      </head>
      <body>
        <div class="header">
          <div class="title-area">
            <h1>Báo Cáo Tổng Kết Công Việc Tuần ✨</h1>
            <p class="subtitle">Trích xuất báo cáo thông minh từ Lovely Scheduler</p>
          </div>
          <div class="meta-info">
            <div class="meta-line">Người lập: <b>David Nguyen</b></div>
            <div class="meta-line">Thời gian: <b>${startDate} - ${endDate}</b></div>
            <div class="meta-line">Ngày tạo: <b>${exportDateStr}</b></div>
          </div>
        </div>

        <div class="task-list">
          ${tasksHtml}
        </div>

        <div class="footer-watermark">
          Báo cáo được khởi tạo tự động bởi ứng dụng Lovely Scheduler. Chúc bạn một tuần mới tràn đầy năng lượng tích cực! ❤️
        </div>
      </body>
      </html>
    `;

    // 4. Sinh file PDF sử dụng expo-print
    const printOptions = {
      html: fullHtml,
      base64: false, // Lưu thành file cục bộ
    };
    
    const { uri: pdfUri } = await Print.printToFileAsync(printOptions);

    // 5. Kích hoạt Popup chia sẻ của hệ thống
    const isAvailable = await Sharing.isAvailableAsync();
    if (!isAvailable) {
      throw new Error('Hệ thống của bạn không hỗ trợ tính năng chia sẻ tệp tin.');
    }
    
    await Sharing.shareAsync(pdfUri, {
      mimeType: 'application/pdf',
      dialogTitle: 'Xuất Báo Cáo Tuần 📈',
      UTI: 'com.adobe.pdf',
    });

    return true;
  } catch (error) {
    console.error('Lỗi khi xuất và chia sẻ tài liệu PDF:', error);
    throw error;
  }
};
