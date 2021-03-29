package sae.iit.saedashboard;

import android.app.Activity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

public class SimpleAnim {

    static void hideViewFade(Activity activity, final View view, int visibility) {
        Animation animation = AnimationUtils.loadAnimation(activity, R.anim.fragment_fade_exit);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                view.setVisibility(visibility);
            }
        });

        view.startAnimation(animation);
    }

    static void showViewFade(Activity activity, final View view) {
        Animation animation = AnimationUtils.loadAnimation(activity, R.anim.fragment_fade_enter);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                view.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
            }
        });

        view.startAnimation(animation);
    }

    static void hideViewLeft(Activity activity, final View view, int visibility) {
        Animation animation = AnimationUtils.loadAnimation(activity, R.anim.slide_in_left);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                view.setVisibility(visibility);
            }
        });

        view.startAnimation(animation);
    }

    static void showViewLeft(Activity activity, final View view) {
        Animation animation = AnimationUtils.loadAnimation(activity, R.anim.slide_out_left);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                view.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
            }
        });

        view.startAnimation(animation);
    }

    static void hideViewRight(Activity activity, final View view, int visibility) {
        Animation animation = AnimationUtils.loadAnimation(activity, R.anim.slide_in_right);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                view.setVisibility(visibility);
            }
        });

        view.startAnimation(animation);
    }

    static void showViewRight(Activity activity, final View view) {
        Animation animation = AnimationUtils.loadAnimation(activity, R.anim.slide_out_right);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                view.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
            }
        });

        view.startAnimation(animation);
    }

    static void hideViewDown(Activity activity, final View view, int visibility) {
        Animation animation = AnimationUtils.loadAnimation(activity, R.anim.slide_in_down);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                view.setVisibility(visibility);
            }
        });

        view.startAnimation(animation);
    }

    static void showViewDown(Activity activity, final View view) {
        Animation animation = AnimationUtils.loadAnimation(activity, R.anim.slide_out_down);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                view.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
            }
        });

        view.startAnimation(animation);
    }

    static void animView(Activity activity, final View view, final int visibility, String direction) {
        if (view.getAnimation() != null)
            return;
        if (visibility != View.VISIBLE) {
            if (view.getVisibility() == visibility)
                return;
            if ((visibility & (View.INVISIBLE | View.GONE)) != 0)
                switch (direction) {
                    case "left":
                        hideViewLeft(activity, view, visibility);
                        return;
                    case "right":
                        hideViewRight(activity, view, visibility);
                        return;
                    case "down":
                        hideViewDown(activity, view, visibility);
                        return;
                    case "fade":
                        hideViewFade(activity, view, visibility);
                }
        } else {
            if (view.getVisibility() == View.VISIBLE)
                return;
            switch (direction) {
                case "left":
                    showViewLeft(activity, view);
                    return;
                case "right":
                    showViewRight(activity, view);
                    return;
                case "down":
                    showViewDown(activity, view);
                    return;
                case "fade":
                    showViewFade(activity, view);
            }
        }
    }
}
