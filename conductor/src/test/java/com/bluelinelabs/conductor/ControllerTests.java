package com.bluelinelabs.conductor;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import com.bluelinelabs.conductor.Controller.RetainViewMode;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class ControllerTests {

    private ActivityProxy activityProxy;
    private Router router;

    public void createActivityController(Bundle savedInstanceState) {
        activityProxy = new ActivityProxy().create(savedInstanceState).start().resume();
        router = Conductor.attachRouter(activityProxy.getActivity(), activityProxy.getView(), savedInstanceState);
        if (!router.hasRootController()) {
            router.setRoot(RouterTransaction.with(new TestController()));
        }
    }

    @Before
    public void setup() {
        createActivityController(null);
    }

    @Test
    public void testViewRetention() {
        Controller controller = new TestController();
        controller.setRouter(router);

        // Test View getting released w/ RELEASE_DETACH
        controller.setRetainViewMode(RetainViewMode.RELEASE_DETACH);
        Assert.assertNull(controller.getView());
        View view = controller.inflate(router.container);
        Assert.assertNotNull(controller.getView());
        ViewUtils.reportAttached(view, true);
        Assert.assertNotNull(controller.getView());
        ViewUtils.reportAttached(view, false);
        Assert.assertNull(controller.getView());

        // Test View getting retained w/ RETAIN_DETACH
        controller.setRetainViewMode(RetainViewMode.RETAIN_DETACH);
        view = controller.inflate(router.container);
        Assert.assertNotNull(controller.getView());
        ViewUtils.reportAttached(view, true);
        Assert.assertNotNull(controller.getView());
        ViewUtils.reportAttached(view, false);
        Assert.assertNotNull(controller.getView());

        // Ensure re-setting RELEASE_DETACH releases
        controller.setRetainViewMode(RetainViewMode.RELEASE_DETACH);
        Assert.assertNull(controller.getView());
    }

    @Test
    public void testActivityResult() {
        TestController controller = new TestController();
        CallState expectedCallState = new CallState(true);

        router.pushController(RouterTransaction.with(controller));

        // Ensure that calling onActivityResult w/o requesting a result doesn't do anything
        router.onActivityResult(1, Activity.RESULT_OK, null);
        assertCalls(expectedCallState, controller);

        // Ensure starting an activity for result gets us the result back
        controller.startActivityForResult(new Intent("action"), 1);
        router.onActivityResult(1, Activity.RESULT_OK, null);
        expectedCallState.onActivityResultCalls++;
        assertCalls(expectedCallState, controller);

        // Ensure requesting a result w/o calling startActivityForResult works
        controller.registerForActivityResult(2);
        router.onActivityResult(2, Activity.RESULT_OK, null);
        expectedCallState.onActivityResultCalls++;
        assertCalls(expectedCallState, controller);
    }

    @Test
    public void testActivityResultForChild() {
        TestController parent = new TestController();
        TestController child = new TestController();

        router.pushController(RouterTransaction.with(parent));
        parent.getChildRouter((ViewGroup)parent.getView().findViewById(TestController.VIEW_ID))
                .setRoot(RouterTransaction.with(child));

        CallState childExpectedCallState = new CallState(true);
        CallState parentExpectedCallState = new CallState(true);

        // Ensure that calling onActivityResult w/o requesting a result doesn't do anything
        router.onActivityResult(1, Activity.RESULT_OK, null);
        assertCalls(childExpectedCallState, child);
        assertCalls(parentExpectedCallState, parent);

        // Ensure starting an activity for result gets us the result back
        child.startActivityForResult(new Intent("action"), 1);
        router.onActivityResult(1, Activity.RESULT_OK, null);
        childExpectedCallState.onActivityResultCalls++;
        assertCalls(childExpectedCallState, child);
        assertCalls(parentExpectedCallState, parent);

        // Ensure requesting a result w/o calling startActivityForResult works
        child.registerForActivityResult(2);
        router.onActivityResult(2, Activity.RESULT_OK, null);
        childExpectedCallState.onActivityResultCalls++;
        assertCalls(childExpectedCallState, child);
        assertCalls(parentExpectedCallState, parent);
    }

    @Test
    public void testPermissionResult() {
        final String[] requestedPermissions = new String[] {"test"};

        TestController controller = new TestController();
        CallState expectedCallState = new CallState(true);

        router.pushController(RouterTransaction.with(controller));

        // Ensure that calling handleRequestedPermission w/o requesting a result doesn't do anything
        router.onRequestPermissionsResult("anotherId", 1, requestedPermissions, new int[] {1});
        assertCalls(expectedCallState, controller);

        // Ensure requesting the permission gets us the result back
        try {
            controller.requestPermissions(requestedPermissions, 1);
        } catch (NoSuchMethodError ignored) { }

        router.onRequestPermissionsResult(controller.getInstanceId(), 1, requestedPermissions, new int[] {1});
        expectedCallState.onRequestPermissionsResultCalls++;
        assertCalls(expectedCallState, controller);
    }

    @Test
    public void testPermissionResultForChild() {
        final String[] requestedPermissions = new String[] {"test"};

        TestController parent = new TestController();
        TestController child = new TestController();

        router.pushController(RouterTransaction.with(parent));
        parent.getChildRouter((ViewGroup)parent.getView().findViewById(TestController.VIEW_ID))
                .setRoot(RouterTransaction.with(child));

        CallState childExpectedCallState = new CallState(true);
        CallState parentExpectedCallState = new CallState(true);

        // Ensure that calling handleRequestedPermission w/o requesting a result doesn't do anything
        router.onRequestPermissionsResult("anotherId", 1, requestedPermissions, new int[] {1});
        assertCalls(childExpectedCallState, child);
        assertCalls(parentExpectedCallState, parent);

        // Ensure requesting the permission gets us the result back
        try {
            child.requestPermissions(requestedPermissions, 1);
        } catch (NoSuchMethodError ignored) { }

        router.onRequestPermissionsResult(child.getInstanceId(), 1, requestedPermissions, new int[] {1});
        childExpectedCallState.onRequestPermissionsResultCalls++;
        assertCalls(childExpectedCallState, child);
        assertCalls(parentExpectedCallState, parent);
    }

    @Test
    public void testOptionsMenu() {
        TestController controller = new TestController();
        CallState expectedCallState = new CallState(true);

        router.pushController(RouterTransaction.with(controller));

        // Ensure that calling onCreateOptionsMenu w/o declaring that we have one doesn't do anything
        router.onCreateOptionsMenu(null, null);
        assertCalls(expectedCallState, controller);

        // Ensure calling onCreateOptionsMenu with a menu works
        controller.setHasOptionsMenu(true);

        // Ensure it'll still get called back next time onCreateOptionsMenu is called
        router.onCreateOptionsMenu(null, null);
        expectedCallState.createOptionsMenuCalls++;
        assertCalls(expectedCallState, controller);

        // Ensure we stop getting them when we hide it
        controller.setOptionsMenuHidden(true);
        router.onCreateOptionsMenu(null, null);
        assertCalls(expectedCallState, controller);

        // Ensure we get the callback them when we un-hide it
        controller.setOptionsMenuHidden(false);
        router.onCreateOptionsMenu(null, null);
        expectedCallState.createOptionsMenuCalls++;
        assertCalls(expectedCallState, controller);

        // Ensure we don't get the callback when we no longer have a menu
        controller.setHasOptionsMenu(false);
        router.onCreateOptionsMenu(null, null);
        assertCalls(expectedCallState, controller);
    }

    @Test
    public void testOptionsMenuForChild() {
        TestController parent = new TestController();
        TestController child = new TestController();

        router.pushController(RouterTransaction.with(parent));
        parent.getChildRouter((ViewGroup)parent.getView().findViewById(TestController.VIEW_ID))
                .setRoot(RouterTransaction.with(child));

        CallState childExpectedCallState = new CallState(true);
        CallState parentExpectedCallState = new CallState(true);

        // Ensure that calling onCreateOptionsMenu w/o declaring that we have one doesn't do anything
        router.onCreateOptionsMenu(null, null);
        assertCalls(childExpectedCallState, child);
        assertCalls(parentExpectedCallState, parent);

        // Ensure calling onCreateOptionsMenu with a menu works
        child.setHasOptionsMenu(true);

        // Ensure it'll still get called back next time onCreateOptionsMenu is called
        router.onCreateOptionsMenu(null, null);
        childExpectedCallState.createOptionsMenuCalls++;
        assertCalls(childExpectedCallState, child);
        assertCalls(parentExpectedCallState, parent);

        // Ensure we stop getting them when we hide it
        child.setOptionsMenuHidden(true);
        router.onCreateOptionsMenu(null, null);
        assertCalls(childExpectedCallState, child);
        assertCalls(parentExpectedCallState, parent);

        // Ensure we get the callback them when we un-hide it
        child.setOptionsMenuHidden(false);
        router.onCreateOptionsMenu(null, null);
        childExpectedCallState.createOptionsMenuCalls++;
        assertCalls(childExpectedCallState, child);
        assertCalls(parentExpectedCallState, parent);

        // Ensure we don't get the callback when we no longer have a menu
        child.setHasOptionsMenu(false);
        router.onCreateOptionsMenu(null, null);
        assertCalls(childExpectedCallState, child);
        assertCalls(parentExpectedCallState, parent);
    }

    @Test
    public void testAddRemoveChildControllers() {
        TestController parent = new TestController();
        TestController child1 = new TestController();
        TestController child2 = new TestController();

        router.pushController(RouterTransaction.with(parent));

        Assert.assertEquals(0, parent.getChildRouters().size());
        Assert.assertNull(child1.getParentController());
        Assert.assertNull(child2.getParentController());

        Router childRouter = parent.getChildRouter((ViewGroup)parent.getView().findViewById(TestController.VIEW_ID));
        childRouter.setPopsLastView(true);
        childRouter.setRoot(RouterTransaction.with(child1));

        Assert.assertEquals(1, parent.getChildRouters().size());
        Assert.assertEquals(childRouter, parent.getChildRouters().get(0));
        Assert.assertEquals(1, childRouter.getBackstackSize());
        Assert.assertEquals(child1, childRouter.getControllers().get(0));
        Assert.assertEquals(parent, child1.getParentController());
        Assert.assertNull(child2.getParentController());

        childRouter = parent.getChildRouter((ViewGroup)parent.getView().findViewById(TestController.VIEW_ID));
        childRouter.pushController(RouterTransaction.with(child2));

        Assert.assertEquals(1, parent.getChildRouters().size());
        Assert.assertEquals(childRouter, parent.getChildRouters().get(0));
        Assert.assertEquals(2, childRouter.getBackstackSize());
        Assert.assertEquals(child1, childRouter.getControllers().get(0));
        Assert.assertEquals(child2, childRouter.getControllers().get(1));
        Assert.assertEquals(parent, child1.getParentController());
        Assert.assertEquals(parent, child2.getParentController());

        childRouter.popController(child2);

        Assert.assertEquals(1, parent.getChildRouters().size());
        Assert.assertEquals(childRouter, parent.getChildRouters().get(0));
        Assert.assertEquals(1, childRouter.getBackstackSize());
        Assert.assertEquals(child1, childRouter.getControllers().get(0));
        Assert.assertEquals(parent, child1.getParentController());
        Assert.assertNull(child2.getParentController());

        childRouter.popController(child1);

        Assert.assertEquals(1, parent.getChildRouters().size());
        Assert.assertEquals(childRouter, parent.getChildRouters().get(0));
        Assert.assertEquals(0, childRouter.getBackstackSize());
        Assert.assertNull(child1.getParentController());
        Assert.assertNull(child2.getParentController());
    }

    @Test
    public void testAddRemoveChildRouters() {
        TestController parent = new TestController();

        TestController child1 = new TestController();
        TestController child2 = new TestController();

        router.pushController(RouterTransaction.with(parent));

        Assert.assertEquals(0, parent.getChildRouters().size());
        Assert.assertNull(child1.getParentController());
        Assert.assertNull(child2.getParentController());

        Router childRouter1 = parent.getChildRouter((ViewGroup)parent.getView().findViewById(TestController.CHILD_VIEW_ID_1));
        Router childRouter2 = parent.getChildRouter((ViewGroup)parent.getView().findViewById(TestController.CHILD_VIEW_ID_2));

        childRouter1.setRoot(RouterTransaction.with(child1));
        childRouter2.setRoot(RouterTransaction.with(child2));

        Assert.assertEquals(2, parent.getChildRouters().size());
        Assert.assertEquals(childRouter1, parent.getChildRouters().get(0));
        Assert.assertEquals(childRouter2, parent.getChildRouters().get(1));
        Assert.assertEquals(1, childRouter1.getBackstackSize());
        Assert.assertEquals(1, childRouter2.getBackstackSize());
        Assert.assertEquals(child1, childRouter1.getControllers().get(0));
        Assert.assertEquals(child2, childRouter2.getControllers().get(0));
        Assert.assertEquals(parent, child1.getParentController());
        Assert.assertEquals(parent, child2.getParentController());

        parent.removeChildRouter(childRouter2);

        Assert.assertEquals(1, parent.getChildRouters().size());
        Assert.assertEquals(childRouter1, parent.getChildRouters().get(0));
        Assert.assertEquals(1, childRouter1.getBackstackSize());
        Assert.assertEquals(0, childRouter2.getBackstackSize());
        Assert.assertEquals(child1, childRouter1.getControllers().get(0));
        Assert.assertEquals(parent, child1.getParentController());
        Assert.assertNull(child2.getParentController());

        parent.removeChildRouter(childRouter1);

        Assert.assertEquals(0, parent.getChildRouters().size());
        Assert.assertEquals(0, childRouter1.getBackstackSize());
        Assert.assertEquals(0, childRouter2.getBackstackSize());
        Assert.assertNull(child1.getParentController());
        Assert.assertNull(child2.getParentController());
    }

    private void assertCalls(CallState callState, TestController controller) {
        Assert.assertEquals("Expected call counts and controller call counts do not match.", callState, controller.currentCallState);
    }

}
