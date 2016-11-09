package com.bluelinelabs.conductor.demo.controllers;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bluelinelabs.conductor.ControllerChangeHandler;
import com.bluelinelabs.conductor.ControllerChangeType;
import com.bluelinelabs.conductor.RouterTransaction;
import com.bluelinelabs.conductor.changehandler.HorizontalChangeHandler;
import com.bluelinelabs.conductor.demo.R;
import com.bluelinelabs.conductor.demo.controllers.base.BaseController;
import com.bluelinelabs.conductor.demo.util.BundleBuilder;
import com.bluelinelabs.conductor.demo.util.ColorUtil;

import butterknife.BindView;
import butterknife.OnClick;

public class NavigationDemoController extends BaseController {

    public static final String TAG_UP_TRANSACTION = "NavigationDemoController.up";

    private static final String KEY_INDEX = "NavigationDemoController.index";
    private static final String KEY_DISPLAY_UP = "NavigationDemoController.displayUp";

    @BindView(R.id.tv_title) TextView tvTitle;

    private int index;
    private boolean displayUp;

    public NavigationDemoController(int index, boolean displayUpButton) {
        this(new BundleBuilder(new Bundle())
                .putInt(KEY_INDEX, index)
                .putBoolean(KEY_DISPLAY_UP, displayUpButton)
                .build());
    }

    public NavigationDemoController(Bundle args) {
        super(args);
        index = args.getInt(KEY_INDEX);
        displayUp = args.getBoolean(KEY_DISPLAY_UP);
    }

    @NonNull
    @Override
    protected View inflateView(@NonNull LayoutInflater inflater, @NonNull ViewGroup container) {
        return inflater.inflate(R.layout.controller_navigation_demo, container, false);
    }

    @Override
    protected void onViewBound(@NonNull View view) {
        super.onViewBound(view);

        if (!displayUp) {
            view.findViewById(R.id.btn_up).setVisibility(View.GONE);
        }

        view.setBackgroundColor(ColorUtil.getMaterialColor(getResources(), index));
        tvTitle.setText(getResources().getString(R.string.navigation_title, index));
    }

    @Override
    protected void onChangeEnded(@NonNull ControllerChangeHandler changeHandler, @NonNull ControllerChangeType changeType) {
        super.onChangeEnded(changeHandler, changeType);

        setButtonsEnabled(true);
    }

    @Override
    protected void onChangeStarted(@NonNull ControllerChangeHandler changeHandler, @NonNull ControllerChangeType changeType) {
        super.onChangeStarted(changeHandler, changeType);

        setButtonsEnabled(false);
    }

    @Override
    protected String getTitle() {
        return "Navigation Demos";
    }

    private void setButtonsEnabled(boolean enabled) {
        final View view = getView();
        if (view != null) {
            view.findViewById(R.id.btn_next).setEnabled(enabled);
            view.findViewById(R.id.btn_up).setEnabled(enabled);
            view.findViewById(R.id.btn_pop_to_root).setEnabled(enabled);
        }
    }

    @OnClick(R.id.btn_next) void onNextClicked() {
        getRouter().pushController(RouterTransaction.with(new NavigationDemoController(index + 1, displayUp))
                .pushChangeHandler(new HorizontalChangeHandler())
                .popChangeHandler(new HorizontalChangeHandler()));
    }

    @OnClick(R.id.btn_up) void onUpClicked() {
        getRouter().popToTag(TAG_UP_TRANSACTION);
    }

    @OnClick(R.id.btn_pop_to_root) void onPopToRootClicked() {
        getRouter().popToRoot();
    }
}
