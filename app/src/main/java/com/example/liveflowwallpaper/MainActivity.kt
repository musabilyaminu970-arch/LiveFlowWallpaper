private fun previewWallpaper() {

    val intent = Intent(
        WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER
    )

    intent.putExtra(
        WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
        ComponentName(
            this,
            FlowWallpaperService::class.java
        )
    )

    startActivity(intent)
}
private fun openPhotoPicker() {

    val intent = Intent(
        Intent.ACTION_OPEN_DOCUMENT
    )

    intent.type = "image/*"
    intent.addCategory(
        Intent.CATEGORY_OPENABLE
    )

    startActivityForResult(
        intent,
        100
    )
}

override fun onActivityResult(
    requestCode: Int,
    resultCode: Int,
    data: Intent?
) {
    super.onActivityResult(
        requestCode,
        resultCode,
        data
    )

    if (
        requestCode == 100 &&
        resultCode == RESULT_OK &&
        data?.data != null
    ) {

        val imageUri = data.data!!

        contentResolver.takePersistableUriPermission(
            imageUri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )

        getSharedPreferences(
            "LiveFlow",
            MODE_PRIVATE
        ).edit()
            .putString(
                "flow_photo",
                imageUri.toString()
            )
            .apply()
    }
}
