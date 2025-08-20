(function (document) {
  const FORM_SEL = 'form.foundation-form[action*="managepublication"]';

  // Wait for the Manage Publication wizard to appear
  function onWizardReady(cb) {
    const f = document.querySelector(FORM_SEL);
    if (f) return cb(f);
    const mo = new MutationObserver(() => {
      const f2 = document.querySelector(FORM_SEL);
      if (f2) { mo.disconnect(); cb(f2); }
    });
    mo.observe(document.documentElement, { childList: true, subtree: true });
  }

  // Build the TZ <select>
  function buildTzField() {
    const wrap = document.createElement('div');
    wrap.className = 'coral-Form-fieldwrapper';
    wrap.style.marginTop = '8px';

    const label = document.createElement('label');
    label.className = 'coral-Form-fieldlabel';
    label.textContent = 'Timezone';

    const select = document.createElement('select');
    select.name = 'myproj.tz';
    select.className = 'coral-Form-field';
    select.required = true;

    // Good starter list (replace with full IANA list via servlet if you prefer)
    [
      'UTC','America/Toronto','America/New_York','America/Chicago','America/Los_Angeles',
      'Europe/London','Europe/Paris','Europe/Berlin','Asia/Kolkata','Asia/Dubai',
      'Asia/Singapore','Australia/Sydney','America/Sao_Paulo','Africa/Johannesburg'
    ].sort().forEach(z => {
      const o = document.createElement('option'); o.value = o.textContent = z; select.appendChild(o);
    });

    const browserTz = Intl.DateTimeFormat().resolvedOptions().timeZone;
    if ([...select.options].some(o => o.value === browserTz)) select.value = browserTz;

    wrap.appendChild(label); wrap.appendChild(select);
    return { wrap, select };
  }

  // Get author server timezone (optional servlet; falls back to browser tz)
  async function getServerZoneId() {
    try {
      const resp = await fetch('/bin/myproj/server-timezone', { credentials: 'include' });
      const json = await resp.json();
      return json.zoneId || Intl.DateTimeFormat().resolvedOptions().timeZone;
    } catch (e) {
      return Intl.DateTimeFormat().resolvedOptions().timeZone;
    }
  }

  // Convert local wall time in tzFrom -> local wall time in tzTo for the same instant
  function convertLocalToLocal(dateStr, timeStr, tzFrom, tzTo) {
    const [y,m,d]   = dateStr.split('-').map(Number);
    const [hh,mm]   = (timeStr || '09:00').split(':').map(Number);
    const utcGuess  = new Date(Date.UTC(y, (m||1)-1, d||1, hh||0, mm||0));

    const partsIn = (tz) => new Intl.DateTimeFormat('en-CA', {
      timeZone: tz, year:'numeric', month:'2-digit', day:'2-digit', hour:'2-digit', minute:'2-digit', hour12:false
    }).formatToParts(utcGuess).reduce((a,p)=> (a[p.type]=p.value, a), {});

    const pf = partsIn(tzFrom);
    const wallFromMs = Date.UTC(+pf.year, +pf.month-1, +pf.day, +pf.hour, +pf.minute);
    const intendedMs = Date.UTC(y, (m||1)-1, d||1, hh||0, mm||0);
    const epochMs    = utcGuess.getTime() + (intendedMs - wallFromMs);

    const dt         = new Date(epochMs);
    const pt         = new Intl.DateTimeFormat('en-CA', {
      timeZone: tzTo, year:'numeric', month:'2-digit', day:'2-digit', hour:'2-digit', minute:'2-digit', hour12:false
    }).formatToParts(dt).reduce((a,p)=> (a[p.type]=p.value, a), {});
    return { date: `${pt.year}-${pt.month}-${pt.day}`, time: `${pt.hour}:${pt.minute}` };
  }

  onWizardReady(async (form) => {
    // OOTB fields
    const whenLater = form.querySelector('coral-radio[name="when"][value="later"], input[name="when"][value="later"]');
    if (!whenLater) return;

    // Activation date field (AEM shows a Coral datepicker; backing input is below)
    const dateInput = form.querySelector('coral-datepicker input, input[type="date"], input[type="datetime-local"], input[name*="date"]');
    if (!dateInput) return;

    // Add a Time input if the wizard doesn't already have one
    let timeInput = form.querySelector('input[type="time"]');
    if (!timeInput) {
      const tw = document.createElement('div');
      tw.className = 'coral-Form-fieldwrapper';
      tw.innerHTML = '<label class="coral-Form-fieldlabel">Activation time</label>' +
                     '<input class="coral-Form-field" type="time" name="myproj.time" required step="300" value="09:00">';
      (dateInput.closest('.coral-Form-fieldwrapper') || dateInput).after(tw);
      timeInput = tw.querySelector('input[type="time"]');
    }

    // Insert the Timezone dropdown right after the time field
    const { wrap, select: tzSelect } = buildTzField();
    (timeInput.closest('.coral-Form-fieldwrapper') || timeInput).after(wrap);

    const serverTz = await getServerZoneId();

    // Convert on submit (only when "Later" is chosen)
    form.addEventListener('submit', () => {
      const chosen = form.querySelector('coral-radio[name="when"][checked], input[name="when"]:checked');
      const when = chosen ? (chosen.value || chosen.getAttribute('value')) : 'now';
      if (when !== 'later') return;

      let dateVal = dateInput.value;
      let timeVal = timeInput.value || '09:00';

      // If date field is datetime-local ("YYYY-MM-DDTHH:mm"), split it
      if (dateVal.includes('T')) { const [d,t] = dateVal.split('T'); dateVal = d; if (!timeVal && t) timeVal = t.slice(0,5); }

      const tzFrom = tzSelect.value || Intl.DateTimeFormat().resolvedOptions().timeZone;
      const { date, time } = convertLocalToLocal(dateVal, timeVal, tzFrom, serverTz);

      // Write back into the actual inputs so AEM schedules correctly in server local time
      if (dateInput.type === 'datetime-local' || dateInput.value.includes('T')) {
        dateInput.value = `${date}T${time}`;
      } else {
        dateInput.value = date;
        if (timeInput) timeInput.value = time;
        const ootbTime = form.querySelector('input[type="time"][name*="time"]');
        if (ootbTime) ootbTime.value = time;
      }
    }, true);
  });
})(document);
