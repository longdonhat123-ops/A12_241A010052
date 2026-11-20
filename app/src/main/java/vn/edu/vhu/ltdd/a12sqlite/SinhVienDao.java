package vn.edu.vhu.ltdd.a12sqlite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

/**
 * DAO (Data Access Object) — nơi DUY NHẤT trong ứng dụng biết tới SQL.
 * Activity chỉ gọi themMoi(), layTatCa()… chứ không tự viết câu lệnh SQL.
 * Nhờ vậy, sau này đổi sang Room hay sang API mạng thì chỉ sửa lớp này.
 */
public class SinhVienDao {

    private final DbHelper helper;

    public SinhVienDao(Context context) {
        this.helper = new DbHelper(context.getApplicationContext());
    }

    /**
     * Thêm một sinh viên.
     * @return id vừa được SQLite cấp, hoặc -1 nếu thất bại (ví dụ trùng mã sinh viên).
     */
    public long themMoi(SinhVien sv) {
        SQLiteDatabase db = helper.getWritableDatabase();

        // ContentValues là cặp "tên cột → giá trị". Dùng nó thay cho việc
        // nối chuỗi SQL bằng dấu + — nối chuỗi dễ sai và dễ bị tấn công SQL injection.
        ContentValues v = new ContentValues();
        v.put(DbHelper.COL_MA_SV, sv.getMaSv());
        v.put(DbHelper.COL_HO_TEN, sv.getHoTen());
        v.put(DbHelper.COL_LOP, sv.getLop());
        v.put(DbHelper.COL_DIEM, sv.getDiem());

        // insert trả về -1 khi vi phạm ràng buộc (ở đây là UNIQUE của ma_sv)
        long id = db.insert(DbHelper.TABLE, null, v);
        if (id != -1) sv.setId(id);
        return id;
    }

    /** Lấy toàn bộ danh sách, sắp xếp theo họ tên. */
    public List<SinhVien> layTatCa() {
        List<SinhVien> ds = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();

        // query(bảng, cột, điều kiện, tham số, groupBy, having, orderBy)
        Cursor c = db.query(DbHelper.TABLE, null, null, null, null, null,
                DbHelper.COL_HO_TEN + " ASC");

        // try-finally để Cursor LUÔN được đóng, kể cả khi có lỗi giữa chừng
        try {
            // Lấy chỉ số cột MỘT LẦN trước vòng lặp cho nhanh
            int iId = c.getColumnIndexOrThrow(DbHelper.COL_ID);
            int iMa = c.getColumnIndexOrThrow(DbHelper.COL_MA_SV);
            int iTen = c.getColumnIndexOrThrow(DbHelper.COL_HO_TEN);
            int iLop = c.getColumnIndexOrThrow(DbHelper.COL_LOP);
            int iDiem = c.getColumnIndexOrThrow(DbHelper.COL_DIEM);

            while (c.moveToNext()) {       // con trỏ bắt đầu TRƯỚC dòng đầu tiên
                ds.add(new SinhVien(
                        c.getLong(iId),
                        c.getString(iMa),
                        c.getString(iTen),
                        c.getString(iLop),
                        c.getDouble(iDiem)));
            }
        } finally {
            c.close();
        }
        return ds;
    }

    /** Đếm số dòng bằng câu truy vấn thô — minh họa rawQuery. */
    public int dem() {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + DbHelper.TABLE, null);
        try {
            return c.moveToFirst() ? c.getInt(0) : 0;
        } finally {
            c.close();
        }
    }

    /** Điểm trung bình của cả lớp — minh họa hàm tổng hợp của SQL. */
    public double diemTrungBinh() {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT AVG(" + DbHelper.COL_DIEM + ") FROM " + DbHelper.TABLE, null);
        try {
            return c.moveToFirst() ? c.getDouble(0) : 0;
        } finally {
            c.close();
        }
    }

    /** Gọi khi không dùng nữa (ví dụ trong onDestroy của Activity). */
    public void dong() {
        helper.close();
    }
}
