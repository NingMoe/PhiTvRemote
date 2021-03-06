package com.phicomm.remotecontrol.modules.main.screenprojection.entity;

import java.io.Serializable;
import java.util.List;

/**
 * 一个目录下的相册对象
 * 改动：PhotoUpImageItem->MItem
 */
public class PhotoUpImageBucket implements Serializable {
    public int mCount = 0;
    public String mBucketName;
    public List<ImageItem> mImageList;

    public int getCount() {
        return mCount;
    }

    public void setCount(int count) {
        this.mCount = count;
    }

    public String getBucketName() {
        return mBucketName;
    }

    public void setBucketName(String bucketName) {
        this.mBucketName = bucketName;
    }

    public List<ImageItem> getImageList() {
        return mImageList;
    }

    public void setImageList(List<ImageItem> imageList) {
        this.mImageList = imageList;
    }
}
