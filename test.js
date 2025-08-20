$(function () {
  console.log("TZ extension (no servlet) loaded ✅");

  /** ======================
   *  CONFIG (edit here)
   *  ====================== */
  // Option A: Hardcode your author server timezone (recommended for accuracy).
  // Examples: "America/Toronto", "UTC", "Europe/London"
  const SERVER_TZ = "America/Toronto";

  // Option B: If you can't hardcode, set to true to use the browser's timezone as a fallback.
  const USE_BROWSER_TZ_IF_UNSET = true;

  // Optional: Choose the default timezone shown to authors (browser TZ is nice).
  const DEFAULT_TZ = Intl.DateTimeFormat().resolvedOptions().timeZone || "UTC";

  /** ======================
   *  Helpers
   *  ====================== */
  function findWizardForm() {
    return $('form.foundation-form').filter(function () {
      return $(this).find('coral-radio[name="when"], input[name="when"]').length > 0;
    }).first();
  }

  function waitForWizard(cb) {
    const $f = findWizardForm();
    if ($f.length) return cb($f);
    const obs = new MutationObserver(() => {
      const $f2 = findWizardForm();
      if ($f2.length) { obs.disconnect(); cb($f2); }
    });
    obs.observe(document.body, { childList: true, subtree: true });
  }

  // Convert local wall time in tzFrom -> local wall time in tzTo for the same instant
  function convertLocalToLocal(dateStr, timeStr, tzFrom, tzTo) {
    const [y, m, d] = dateStr.split('-').map(Number);
    const [hh, mm] = (timeStr || '09:00').split(':').map(Number);
    const guessUTC = new Date(Date.UTC(y, (m || 1) - 1, d || 1, hh || 0, mm || 0));

    const parts = (tz, dt) => new Intl.DateTimeFormat('en-CA', {
      timeZone: tz, year:'numeric', month:'2-digit', day:'2-digit',
      hour:'2-digit', minute:'2-digit', hour12:false
    }).formatToParts(dt).reduce((a,p)=>(a[p.type]=p.value, a), {});

    const fromP = parts(tzFrom, guessUTC);
    const wallFrom = Date.UTC(+fromP.year, +fromP.month-1, +fromP.day, +fromP.hour, +fromP.minute);
    const intended = Date.UTC(y, (m||1)-1, d||1, hh||0, mm||0);
    const epoch = guessUTC.getTime() + (intended - wallFrom);

    const atInstant = new Date(epoch);
    const toP = parts(tzTo, atInstant);
    return { date: `${toP.year}-${toP.month}-${toP.day}`, time: `${toP.hour}:${toP.minute}` };
  }

  function buildTzSelect(zones, defaultTz) {
    const $sel = $('<select class="coral-Form-field" required></select>');
    zones.forEach(z => $sel.append($('<option>').val(z).text(z)));
    if (zones.indexOf(defaultTz) >= 0) $sel.val(defaultTz);
    return $sel;
  }

  /** ======================
   *  Main
   *  ====================== */
  waitForWizard(function ($form) {
    console.log("Manage Publication wizard form found ✅");

    // Date input from Coral DatePicker
    const $dateInput = $form.find('coral-datepicker input, input[type="date"], input[name*="date"]').first();
    const $whenRadios = $form.find('coral-radio[name="when"], input[name="when"]');
    if (!$dateInput.length || !$whenRadios.length) return;

    // Add Time input (AEM wizard usually lacks one)
    let $timeInput = $form.find('input[type="time"]').first();
    if (!$timeInput.length) {
      const $timeWrap = $(`
        <div class="coral-Form-fieldwrapper">
          <label class="coral-Form-fieldlabel">Activation time</label>
          <input class="coral-Form-field" type="time" step="300" value="09:00"/>
        </div>`);
      ($dateInput.closest('.coral-Form-fieldwrapper').length ? $dateInput.closest('.coral-Form-fieldwrapper') : $dateInput)
        .after($timeWrap);
      $timeInput = $timeWrap.find('input[type="time"]');
    }

    // Add Timezone dropdown
    const zones = [
      'UTC','America/Toronto','America/New_York','America/Chicago','America/Los_Angeles',
      'Europe/London','Europe/Paris','Europe/Berlin','Asia/Kolkata','Asia/Dubai',
      'Asia/Singapore','Australia/Sydney','America/Sao_Paulo','Africa/Johannesburg'
    ].sort();

    const $tzSelect = buildTzSelect(zones, DEFAULT_TZ);
    const $tzWrap = $(`
      <div class="coral-Form-fieldwrapper">
        <label class="coral-Form-fieldlabel">Timezone</label>
      </div>`).append($tzSelect);
    $timeInput.closest('.coral-Form-fieldwrapper').after($tzWrap);

    // Determine target "server" timezone without servlet
    const targetServerTz = (SERVER_TZ && SERVER_TZ.trim().length) ? SERVER_TZ : (
      USE_BROWSER_TZ_IF_UNSET ? (Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC') : 'UTC'
    );

    // Intercept submit even if the form has no action
    $(document).on('submit', 'form.foundation-form', function () {
      const $f = $(this);
      if ($f.get(0) !== $form.get(0)) return; // ensure it's the same wizard

      // Only when "Later" is selected
      const $checked = $f.find('coral-radio[name="when"][checked], input[name="when"]:checked').first();
      const whenVal = $checked.length ? ($checked.val() || $checked.attr('value')) : 'now';
      if (whenVal !== 'later') return;

      const d = $dateInput.val();        // "YYYY-MM-DD"
      const t = $timeInput.val() || '09:00';
      if (!d) return;

      const tzFrom = $tzSelect.val();
      const tzTo   = targetServerTz;

      const { date, time } = convertLocalToLocal(d, t, tzFrom, tzTo);

      // Write back so OOTB scheduler stores server-local wall time
      $dateInput.val(date);
      $timeInput.val(time);

      console.log(`TZ adjust: ${d} ${t} ${tzFrom} → ${date} ${time} (${tzTo})`);
    });
  });
});
