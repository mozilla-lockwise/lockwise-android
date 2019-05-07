# Adding icons/images assets into project

Lockwise app uses 2 types of assets for icons/images that are added in the project: Vector Assets and .png raster images. 

Using vector drawables instead of flat images reduces the size of the APK, the same file is being resized for different screen densities without loss of image quality and also it is easier to maintain one XML file instead of updating multiple raster graphics at various resolutions.
However, when adding an image asset to the project, we should take into consideration the following aspects:

- Vector assets can be added using Vector Asset Studio tool in Android Studio. The tool imports an .svg file (Scalable Vector Graphic) into the project as a vector drawable resource. (The .svg file is available in Zeplin)
  - Simple icons with few details that are intended for small display size (max 200x200dp) should use vector assets. 
  - Vector Assets should be added to one the following folders in the application:
    - pp->res->drawable->drawable: for simple icons from Android libraries
    - pp->res->drawable->drawable-v24: for more complex images (e.g. the Lockwise logo)
  - To open Vector Asset Studio tool, go to the appropriate folder in Android View, right click on drawable folder, and select New -> Vector Asset
  - Vector Asset Studio opens. Select Local file, specify the .svg image file path, override the Size and optionally change icon name. Then select Next and Finish
  - More information related to Vector Drawables and Vector Asset Studio can be found [here](https://developer.android.com/studio/write/vector-asset-studio)
- If the image is more complex, has a larger size, or the vector drawable contains attributes that are not supported starting from the minSdk level set in the project (currently minSdk api level is 23)--for example `fillType` or `gradient` attributes--we should consider adding flat images for different resolutions.
  - The .png files are also available in Zeplin and they have to be added in res->drawable specific folders: `drawable-mdpi`, `drawable-hdpi`, `drawable-xhdpi`, `drawable-xxhdpi`.
  - Download the .png files and add each of the images to their corresponding res->drawable folder in Android View
  - If you wish to add both a Vector Asset and .png assets to your project, ensure that all versions of the same image have the same file name (e.g. `ic_default_avatar`).
