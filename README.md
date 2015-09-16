# EasyImageSharing
If you need to implement the common pick-image-and-share-it functionality in your Android application, this library helps you to integrate it in just 3 lines of code

### Features
- First dialog with "Take photo" / "Pick from gallery" options
- Optionally place a text watermark
- Preview of the image with EditText for optional message
- Check if external storage is available
- Callback for error handle
- English and Spanish translations

### Getting started

Instance EasyImageSharing and launch the dialog:
```java
imageSharing = EasyImageSharing.newInstance(this);
imageSharing.takeImageAndShare();
```

And **DON'T FORGET** to notify the library OnActivityResult event on your Activity or Fragment
```java
@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    imageSharing.onActivityResult(requestCode,  resultCode,  data);
}
```


Optionally you can place a watermark or set the error listener
```java
imageSharing.setWatermark("+1");
imageSharing.setOnErrorListener(new EasyImageSharing.OnErrorListener() {
    @Override
    public void onError(String reason) {
        // Do your stuff
    }
});
```

#### Want to test it by yourself? [Install the APK](EasyImageShareing-example.apk)

### Enjoy!
