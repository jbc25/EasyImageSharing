package es.triskelapps.easyimagesharing;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import es.triskelapps.easyimagesharing.util.BitmapUtils;

/**
 * Created by julio on 30/04/15.
 */
public class EasyImageSharing {

    public static final int REQ_CODE_TAKE_PHOTO = 1234;
    public static final int REQ_CODE_PICK_IMAGE = 1235;
    public static final int REQ_CODE_SHARE = 1236;
    private static final String TAG = "EasyImageSharing";

    private final Activity mActivity;
    private final Fragment mFragment;
    private final boolean isSDCardAvailable;
    private File mFileImage;
    private String IMAGE_NAME_DEFAULT = "Captured.jpg";
    private String mWatermark;
    private OnErrorListener onErrorListener;
    private String message;

    public static EasyImageSharing newInstance(Activity activity) {
        return new EasyImageSharing(activity, null);
    }

    public static EasyImageSharing newInstance(Activity activity, Fragment fragment) {
        return new EasyImageSharing(activity, fragment);
    }

    public EasyImageSharing(Activity activity, Fragment fragment) {

        if (activity == null) {
            throw new IllegalArgumentException("You must provide an activity");
        }

        mActivity = activity;
        mFragment = fragment;

//        mFileImage = new File(mActivity.getFilesDir() + "/" + IMAGE_NAME_DEFAULT);

        isSDCardAvailable = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);

        if (!isSDCardAvailable) {
            showSdCardNotAvailableError();
            return;
        }


        mFileImage = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                IMAGE_NAME_DEFAULT);

    }

    private Context getContext() {
        return mActivity;
    }

    public void takeImageAndShare() {
        showPickDialog();
    }

    public void setWatermark(String watermark) {
        this.mWatermark = watermark;
    }


    private void showPickDialog() {

        String[] items = getContext().getResources().getStringArray(
                R.array.options);


        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(),
                android.R.layout.simple_list_item_1, items);

        AlertDialog.Builder alert = new AlertDialog.Builder(getContext());
        alert.setTitle(R.string.select)
                .setAdapter(adapter, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {

                        switch (which) {
                            case 0: // Take photo from camera
                                takePhoto();
                                break;

                            case 1:
                                pickFromGallery();
                                break;

                        }

                    }
                }).show();

    }

    private void takePhoto() {

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT,
                Uri.fromFile(mFileImage));

        if (mFragment != null) {
            mFragment.startActivityForResult(intent, REQ_CODE_TAKE_PHOTO);
        } else {
            mActivity.startActivityForResult(intent, REQ_CODE_TAKE_PHOTO);
        }

    }

    private void pickFromGallery() {

        Intent intent = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

        if (mFragment != null) {
            mFragment.startActivityForResult(intent, REQ_CODE_PICK_IMAGE);
        } else {
            mActivity.startActivityForResult(intent, REQ_CODE_PICK_IMAGE);
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {


        switch (requestCode) {
            case REQ_CODE_TAKE_PHOTO:

                if (resultCode != Activity.RESULT_OK) {
                    return;
                }

                Bitmap rotatedBitmap = null;

                rotatedBitmap = BitmapUtils.getRotatedBitmap(mFileImage.getPath());

                if (rotatedBitmap != null) {

                    alertConfirmPhoto(rotatedBitmap, mFileImage);
                } else {
                    Log.e(TAG, "rotatedBitmap = null");

                    showImageError();
                }

                break;

            case REQ_CODE_PICK_IMAGE:

                if (resultCode != Activity.RESULT_OK) {
                    return;
                }

                Uri selectedImage = data.getData();
                String[] filePathColumn = {MediaStore.Images.Media.DATA};

                Cursor cursor = getContext().getContentResolver().query(selectedImage,
                        filePathColumn, null, null, null);
                cursor.moveToFirst();

                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                final String filePath = cursor.getString(columnIndex);
                cursor.close();

                Bitmap rotatedBitmap1 = BitmapUtils.getRotatedBitmap(filePath);

                if (rotatedBitmap1 != null) {

                    alertConfirmPhoto(rotatedBitmap1, mFileImage);

                } else {
                    Log.e(TAG, "rotatedBitmap = null");
                    showImageError();
                }
                break;

            case REQ_CODE_SHARE:

                deleteFile();
                message = null;
                break;
        }

    }



    private void alertConfirmPhoto(Bitmap photo, final File fileImage) {

        final Bitmap imageToSave = mWatermark != null ? mark(photo, mWatermark) : photo;

        View viewConfirm = View.inflate(getContext(), R.layout.view_confirm, null);

        ImageView img_foto = (ImageView) viewConfirm.findViewById(R.id.image);
        img_foto.setImageBitmap(imageToSave);

        final EditText editMessage = (EditText) viewConfirm.findViewById(R.id.edit_message);

        AlertDialog.Builder alert = new AlertDialog.Builder(getContext());
        alert.setTitle(R.string.send)
                .setView(viewConfirm)
                .setPositiveButton(R.string.accept,
                        new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog,
                                                int which) {


                                // Save in files directory
                                try {
                                    OutputStream out = new FileOutputStream(mFileImage);
                                    imageToSave.compress(Bitmap.CompressFormat.JPEG,
                                            100, out);
                                    out.flush();
                                    out.close();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    Toast.makeText(getContext(), "Could not save image", Toast.LENGTH_LONG).show();
                                }


                                message = editMessage.getText().toString().trim();
                                shareImage();


                            }
                        })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteFile();
                    }
                }).show();
    }

    private void deleteFile() {

        if (mFileImage.isFile()) {
            mFileImage.delete();
        }
    }

    public Bitmap mark(Bitmap src, String watermark) {

        int w = src.getWidth();
        int h = src.getHeight();
        Bitmap result = Bitmap.createBitmap(w, h, src.getConfig());
        Canvas canvas = new Canvas(result);
        canvas.drawBitmap(src, 0, 0, null);
        Paint paint = new Paint();
        paint.setColor(watermark.equals("-1") ? Color.WHITE : Color.WHITE);
        paint.setTextSize(getContext().getResources().getDimensionPixelSize(R.dimen.text_watermark));
        paint.setAntiAlias(true);
        paint.setAlpha(150);

        Rect bounds = new Rect();
        paint.getTextBounds(watermark, 0, watermark.length(), bounds);

        int x = w / 2 - bounds.width() / 2;
        int y = h / 2;  // - bounds.height()/2;

        canvas.drawText(watermark, x, y, paint);

        return result;
    }

    private void shareImage() {

        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(mFileImage));
        if (message != null && !message.isEmpty()) {
            shareIntent.putExtra(Intent.EXTRA_TEXT, message);
        }
        shareIntent.setType("image/jpeg");

        Intent intent = Intent.createChooser(shareIntent,
                getContext().getResources().getText(R.string.share_with));

        if (mFragment != null) {
            mFragment.startActivityForResult(intent, REQ_CODE_SHARE);
        } else {
            mActivity.startActivityForResult(intent, REQ_CODE_SHARE);
        }

    }


    public interface OnErrorListener {
        void onError(String reason);
    }

    public void setOnErrorListener(OnErrorListener listener) {
        this.onErrorListener = listener;
    }


    private void showSdCardNotAvailableError() {

        if (onErrorListener != null) {
            onErrorListener.onError("External storage not available");
        }
    }

    private void showImageError() {

        if (onErrorListener != null) {
            onErrorListener.onError("Error processing image");
        }
    }
}
