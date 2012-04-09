/*
 * Copyright 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// ActionBarCompatより

package com.example.android.actionbarcompat;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import android.app.Activity;
import android.content.Context;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.InflateException;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * A class that implements the action bar pattern for pre-Honeycomb devices.
 */
public class ActionBarHelperBase extends ActionBarHelper {
    private static final String MENU_RES_NAMESPACE = "http://schemas.android.com/apk/res/android";
    private static final String MENU_ATTR_ID = "id";
    private static final String MENU_ATTR_SHOW_AS_ACTION = "showAsAction";

    protected Set<Integer> mActionItemIds = new HashSet<Integer>();

    protected ActionBarHelperBase(ActionBarActivity activity) {
        super(activity);
    }

    /** {@inheritDoc} */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (mNoTitle) {
            super.onCreate(savedInstanceState);
        } else {
            mActivity.requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onPostCreate(Bundle savedInstanceState) {
        mActivity.getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,
                R.layout.actionbar);
        invalidateMenu();
    }

    /**
     * Sets up the compatibility action bar with the given title.
     */
    private void setupActionBar() {
        final ViewGroup actionBarCompat = getActionBarCompat();
        if (actionBarCompat == null) {
            return;
        }
        actionBarCompat.removeAllViews();

        LinearLayout.LayoutParams springLayoutParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT);
        springLayoutParams.weight = 1;

        // Add Home button
        SimpleMenu tempMenu = new SimpleMenu(mActivity);
        SimpleMenuItem homeItem = new SimpleMenuItem(tempMenu,
                android.R.id.home, 0,
                mActivity.getString(R.string.actionbar_title));
        homeItem.setIcon(mActivity.getHomeIconRes());
        addActionItemCompatFromMenuItem(homeItem);

        // Add title text
        TextView titleText = new TextView(mActivity, null,
                R.attr.actionbarCompatTitleStyle);
        titleText.setLayoutParams(springLayoutParams);
        titleText.setText(mActivity.getTitle());
        titleText.setGravity(android.view.Gravity.CENTER_VERTICAL);
        actionBarCompat.addView(titleText);
    }

    @Override
    public void invalidateMenu() {
        setupActionBar();

        mActionItemIds.clear();
        SimpleMenu menu = new SimpleMenu(mActivity);
        mActivity.onCreatePanelMenu(Window.FEATURE_OPTIONS_PANEL, menu);
        mActivity.onPrepareOptionsMenu(menu);

        boolean hasmenu = false;
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            if (mActionItemIds.contains(item.getItemId())) {
                addActionItemCompatFromMenuItem(item);
            } else if (item.isVisible()) {
                hasmenu = true;
            }
        }

        // Add menu button
        if (hasmenu) {
            SimpleMenu tempMenu = new SimpleMenu(mActivity);
            SimpleMenuItem menuItem = new SimpleMenuItem(tempMenu,
                    R.drawable.ic_menu_moreoverflow, 0, null);
            menuItem.setIcon(R.drawable.ic_menu_moreoverflow);
            addActionItemCompatFromMenuItem(menuItem);
        }
    }

    public void setHomeIcon(int resId) {
        ImageButton home = getHomeIcon();
        if (home != null) {
            home.setImageResource(resId);
        }
    }

    public void setHomeIcon(Drawable logo) {
        ImageButton home = getHomeIcon();
        if (home != null) {
            home.setImageDrawable(logo);
        }
    }

    private ImageButton getHomeIcon() {
        ViewGroup actionBar = getActionBarCompat();
        if (actionBar != null) {
            return (ImageButton)actionBar.findViewById(android.R.id.home);
        } else {
            return null;
        }
    }

    /**
     * Action bar helper code to be run in
     * {@link Activity#onCreateOptionsMenu(android.view.Menu)}.
     *
     * NOTE: This code will mark on-screen menu items as invisible.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Hides on-screen action items from the options menu.
        for (Integer id : mActionItemIds) {
            menu.findItem(id).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.drawable.ic_menu_moreoverflow) {
            // Open menu
            mActivity.openOptionsMenu();
            return true;
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    protected void onTitleChanged(CharSequence title, int color) {
        TextView titleView = (TextView)mActivity
                .findViewById(R.id.actionbar_compat_title);
        if (titleView != null) {
            titleView.setText(title);
        }
    }

    /**
     * Returns a {@link android.view.MenuInflater} that can read action bar
     * metadata on pre-Honeycomb devices.
     */
    public MenuInflater getMenuInflater(MenuInflater superMenuInflater) {
        return new WrappedMenuInflater(mActivity, superMenuInflater);
    }

    /**
     * Returns the {@link android.view.ViewGroup} for the action bar on phones
     * (compatibility action bar). Can return null, and will return null on
     * Honeycomb.
     */
    private ViewGroup getActionBarCompat() {
        return (ViewGroup)mActivity.findViewById(R.id.actionbar_compat);
    }

    /**
     * Adds an action button to the compatibility action bar, using menu
     * information from a {@link android.view.MenuItem}. If the menu item ID is
     * <code>menu_refresh</code>, the menu item's state can be changed to show a
     * loading spinner using .
     */
    private View addActionItemCompatFromMenuItem(final MenuItem item) {
        final int itemId = item.getItemId();

        final ViewGroup actionBar = getActionBarCompat();
        if (actionBar == null) {
            return null;
        }

        int style;
        LinearLayout.LayoutParams params;
        if (itemId == android.R.id.home) {
            style = R.attr.actionbarCompatItemHomeStyle;
            params = new LinearLayout.LayoutParams(
                    getDimension(R.dimen.actionbar_compat_button_home_width),
                    ViewGroup.LayoutParams.MATCH_PARENT);
        } else {
            style = R.attr.actionbarCompatItemStyle;
            params = new LinearLayout.LayoutParams(
                    getDimension(R.dimen.actionbar_compat_button_width),
                    ViewGroup.LayoutParams.MATCH_PARENT);
            params.setMargins(
                    getDimension(R.dimen.actionbar_compat_button_margin), 0,
                    getDimension(R.dimen.actionbar_compat_button_margin), 0);
        }

        // Create the button
        ImageButton actionButton = new ImageButton(mActivity, null, style);
        actionButton.setLayoutParams(params);
        actionButton.setId(itemId);
        actionButton.setImageDrawable(item.getIcon());
        actionButton.setScaleType(ImageView.ScaleType.FIT_CENTER);
        actionButton.setContentDescription(item.getTitle());
        actionButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                mActivity
                        .onMenuItemSelected(Window.FEATURE_OPTIONS_PANEL, item);
            }
        });

        actionBar.addView(actionButton);

        return actionButton;
    }

    private int getDimension(int resid) {
        return (int)mActivity.getResources().getDimension(resid);
    }

    /**
     * A {@link android.view.MenuInflater} that reads action bar metadata.
     */
    private class WrappedMenuInflater extends MenuInflater {
        MenuInflater mInflater;

        public WrappedMenuInflater(Context context, MenuInflater inflater) {
            super(context);
            mInflater = inflater;
        }

        @Override
        public void inflate(int menuRes, Menu menu) {
            loadActionBarMetadata(menuRes);
            mInflater.inflate(menuRes, menu);
        }

        /**
         * Loads action bar metadata from a menu resource, storing a list of
         * menu item IDs that should be shown on-screen (i.e. those with
         * showAsAction set to always or ifRoom).
         *
         * @param menuResId
         */
        private void loadActionBarMetadata(int menuResId) {
            XmlResourceParser parser = null;
            try {
                parser = mActivity.getResources().getXml(menuResId);

                int eventType = parser.getEventType();
                int itemId;
                int showAsAction;

                boolean eof = false;
                while (!eof) {
                    switch (eventType) {
                    case XmlPullParser.START_TAG:
                        if (!parser.getName().equals("item")) {
                            break;
                        }

                        itemId = parser.getAttributeResourceValue(
                                MENU_RES_NAMESPACE, MENU_ATTR_ID, 0);
                        if (itemId == 0) {
                            break;
                        }

                        showAsAction = parser.getAttributeIntValue(
                                MENU_RES_NAMESPACE, MENU_ATTR_SHOW_AS_ACTION,
                                -1);
                        if (showAsAction == MenuItem.SHOW_AS_ACTION_ALWAYS || false/*
                                                                                     * showAsAction
                                                                                     * ==
                                                                                     * MenuItem
                                                                                     * .
                                                                                     * SHOW_AS_ACTION_IF_ROOM
                                                                                     * ifRoomはnever扱い
                                                                                     */) {
                            mActionItemIds.add(itemId);
                        }
                        break;

                    case XmlPullParser.END_DOCUMENT:
                        eof = true;
                        break;
                    }

                    eventType = parser.next();
                }
            } catch (XmlPullParserException e) {
                throw new InflateException("Error inflating menu XML", e);
            } catch (IOException e) {
                throw new InflateException("Error inflating menu XML", e);
            } finally {
                if (parser != null) {
                    parser.close();
                }
            }
        }

    }
}
