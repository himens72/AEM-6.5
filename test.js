(function ($, Granite) {
  "use strict";

  function toggle($checkbox) {
    var targetSelector = $checkbox.data("cqDialogCheckboxShowhideTarget");
    if (!targetSelector) return;

    var $target = $(targetSelector);
    // If you prefer to only hide the field wrapper, uncomment next line:
    // $target = $target.closest(".coral-Form-fieldwrapper").length ? $target.closest(".coral-Form-fieldwrapper") : $target;

    if ($checkbox.prop("checked")) {
      $target.show();
    } else {
      $target.hide();
    }
  }

  // Initialize on dialog load
  $(document).on("foundation-contentloaded", function (e) {
    $(e.target).find(".cq-dialog-checkbox-showhide").each(function () {
      toggle($(this));
    });
  });

  // React to user changes
  $(document).on("change", ".cq-dialog-checkbox-showhide", function () {
    toggle($(this));
  });
})(Granite.$, Granite);
