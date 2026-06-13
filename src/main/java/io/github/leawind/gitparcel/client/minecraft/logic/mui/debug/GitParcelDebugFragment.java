package io.github.leawind.gitparcel.client.minecraft.logic.mui.debug;

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.fragment.Fragment;
import icyllis.modernui.mc.ScreenCallback;
import icyllis.modernui.mc.ui.ClampingScrollView;
import icyllis.modernui.util.DataSet;
import icyllis.modernui.view.Gravity;
import icyllis.modernui.view.LayoutInflater;
import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewGroup;
import icyllis.modernui.widget.LinearLayout;
import icyllis.modernui.widget.PagerAdapter;
import icyllis.modernui.widget.ScrollView;
import icyllis.modernui.widget.TabLayout;
import icyllis.modernui.widget.ViewPager;

public class GitParcelDebugFragment extends Fragment implements ScreenCallback {

  private TabLayout mTabLayout;
  private ViewPager mViewPager;
  private MetadataPanelView mMetadataPanel;
  private ParcelsPanelView mParcelsPanel;
  private ControlPanelView mControlPanel;

  public GitParcelDebugFragment() {}

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable DataSet savedInstanceState) {
    var context = requireContext();
    var root = new LinearLayout(context);
    root.setOrientation(LinearLayout.VERTICAL);

    mTabLayout = new TabLayout(context);
    root.addView(mTabLayout);

    mViewPager = new ViewPager(context);
    mViewPager.setAdapter(new DebugPagerAdapter());
    mTabLayout.setupWithViewPager(mViewPager);

    var lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 0, 1.0f);
    root.addView(mViewPager, lp);

    return root;
  }

  @Override
  public void onResume() {
    super.onResume();
    if (mMetadataPanel != null) {
      mMetadataPanel.refresh();
    }
    if (mParcelsPanel != null) {
      mParcelsPanel.refresh();
    }
    if (mControlPanel != null) {
      mControlPanel.refresh();
    }
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    mTabLayout = null;
    mViewPager = null;
    mMetadataPanel = null;
    mParcelsPanel = null;
    mControlPanel = null;
  }

  @Override
  public boolean isPauseScreen() {
    return false;
  }

  private class DebugPagerAdapter extends PagerAdapter {

    private static final int PAGE_COUNT = 3;

    private final CharSequence[] mTitles = {"Metadata", "Parcels", "Control"};

    @Override
    public int getCount() {
      return PAGE_COUNT;
    }

    @NonNull
    @Override
    public CharSequence getPageTitle(int position) {
      return mTitles[position];
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
      var context = container.getContext();
      final int dp20 = container.dp(20);
      final int maxWidth = container.dp(800) + dp20 + dp20;

      var sv = new ClampingScrollView(context);
      sv.setChildMaxWidth(maxWidth);

      View content =
          switch (position) {
            case 0 -> {
              mMetadataPanel = MetadataPanelView.create(context);
              yield mMetadataPanel;
            }
            case 1 -> {
              mParcelsPanel = new ParcelsPanelView(context);
              yield mParcelsPanel;
            }
            case 2 -> {
              mControlPanel = new ControlPanelView(context);
              yield mControlPanel;
            }
            default -> throw new IllegalStateException();
          };

      content.setPadding(dp20, 0, dp20, 0);
      var params =
          new ScrollView.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT,
              ViewGroup.LayoutParams.WRAP_CONTENT,
              Gravity.CENTER_HORIZONTAL);
      sv.addView(content, params);

      container.addView(sv);
      return sv;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
      container.removeView((View) object);
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
      return view == object;
    }
  }
}
