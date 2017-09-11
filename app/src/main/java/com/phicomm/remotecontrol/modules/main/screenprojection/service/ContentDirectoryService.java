package com.phicomm.remotecontrol.modules.main.screenprojection.service;

import com.phicomm.remotecontrol.modules.main.screenprojection.entity.ContentNode;
import com.phicomm.remotecontrol.modules.main.screenprojection.entity.ContentTree;
import com.phicomm.remotecontrol.util.LogUtil;

import org.fourthline.cling.support.contentdirectory.AbstractContentDirectoryService;
import org.fourthline.cling.support.contentdirectory.ContentDirectoryErrorCode;
import org.fourthline.cling.support.contentdirectory.ContentDirectoryException;
import org.fourthline.cling.support.contentdirectory.DIDLParser;
import org.fourthline.cling.support.model.BrowseFlag;
import org.fourthline.cling.support.model.BrowseResult;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.SortCriterion;
import org.fourthline.cling.support.model.container.Container;
import org.fourthline.cling.support.model.item.Item;

/**
 * Created by kang.sun on 2017/8/21.
 */
public class ContentDirectoryService extends AbstractContentDirectoryService {
    private final static String TAG = ContentDirectoryService.class
            .getSimpleName();

    @Override
    public BrowseResult browse(String objectID, BrowseFlag browseFlag,
                               String filter, long firstResult, long maxResults,
                               SortCriterion[] orderby) throws ContentDirectoryException {
        try {
            DIDLContent didl = new DIDLContent();
            ContentNode contentNode = ContentTree.getNode(objectID);
            LogUtil.v(TAG, "someone's browsing id: " + objectID);
            if (contentNode == null)
                return new BrowseResult("", 0, 0);
            if (contentNode.isItem()) {
                didl.addItem(contentNode.getItem());
                LogUtil.v(TAG, "returing item: " + contentNode.getItem().getTitle());
                return new BrowseResult(new DIDLParser().generate(didl), 1, 1);
            } else {
                if (browseFlag == BrowseFlag.METADATA) {
                    didl.addContainer(contentNode.getContainer());
                    LogUtil.v(TAG, "returning metadata of container: " + contentNode.getContainer().getTitle());
                    return new BrowseResult(new DIDLParser().generate(didl), 1,
                            1);
                } else {
                    for (Container container : contentNode.getContainer()
                            .getContainers()) {
                        didl.addContainer(container);
                        LogUtil.v(TAG, "getting child container: " + container.getTitle());
                    }
                    for (Item item : contentNode.getContainer().getItems()) {
                        didl.addItem(item);
                        LogUtil.v(TAG, "getting child item: " + item.getTitle());
                    }
                    return new BrowseResult(new DIDLParser().generate(didl),
                            contentNode.getContainer().getChildCount(),
                            contentNode.getContainer().getChildCount());
                }
            }
        } catch (Exception ex) {
            throw new ContentDirectoryException(
                    ContentDirectoryErrorCode.CANNOT_PROCESS, ex.toString());
        }
    }
}

