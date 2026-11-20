(() => {
  const data = document.querySelector('[data-mission-data]');
  const missions = JSON.parse(data.textContent);
  const root = data.previousElementSibling;
  const find = selector => root.querySelector(selector);
  const readSave = key => {
    try { return localStorage.getItem(key); } catch { return null; }
  };
  const stars = missions.map(mission => {
    const saved = mission.stars && readSave(mission.stars);
    return /^[1-3]$/.test(saved || '') ? Number(saved) : 0;
  });
  const endpoints = missions.flatMap((entry, index) => entry.endpoint ? [stars[index]] : []);
  const stageRating = mission => {
    const saved = mission.stageStars && readSave(mission.stageStars);
    return /^[1-3]$/.test(saved || '') ? Number(saved) : 0;
  };
  const ratingText = rating => '★'.repeat(rating) + '☆'.repeat(3 - rating) + ' · ' + rating + ' / 3';
  const number = index => String(index + 1).padStart(2, '0');
  const requested = Number(new URLSearchParams(location.search).get('op'));
  let selected = Number.isInteger(requested) && requested >= 1 && requested <= missions.length
    ? requested - 1 : Math.max(0, missions.findIndex((mission, index) => mission.href && !stars[index]));
  const picker = find('[data-chapter-picker]');
  const scroller = find('.gc-map-scroll');
  for (const [index, mission] of missions.entries()) {
    picker.add(new Option(number(index) + ' · ' + mission.title + (mission.href ? '' : '（未开放）'), String(index)));
  }
  picker.onchange = () => { selected = Number(picker.value); render(); };

  const pins = missions.map((mission, index) => {
    const pin = document.createElement('button');
    const rating = stars[index] ? '★'.repeat(stars[index]) + '☆'.repeat(3 - stars[index]) : '';
    const status = rating || (mission.stage && stageRating(mission) ? '第一阶段已完成，城区阶段可出击' : mission.href ? '可出击' : '未开放');
    pin.type = 'button';
    pin.className = 'gc-pin ' + (mission.href ? stars[index] ? 'gc-complete' : 'gc-current' : 'gc-locked');
    pin.classList.toggle('gc-fiction', !!mission.fiction);
    pin.textContent = number(index);
    pin.setAttribute('aria-label', number(index) + ' ' + mission.title + '，' + status);
    pin.title = mission.title;
    pin.style.left = mission.point[0] + '%';
    pin.style.top = mission.point[1] + '%';
    pin.onclick = () => { selected = index; render(); };
    find('[data-map-pins]').append(pin);
    return pin;
  });

  function render() {
    const mission = missions[selected];
    const enter = find('[data-enter]');
    pins.forEach((pin, index) => pin.setAttribute('aria-pressed', String(index === selected)));
    picker.value = String(selected);
    const pin = pins[selected].getBoundingClientRect();
    const viewport = scroller.getBoundingClientRect();
    if (pin.left < viewport.left || pin.right > viewport.right) {
      scroller.scrollLeft += (pin.left + pin.right - viewport.left - viewport.right) / 2;
    }
    find('[data-map-title]').textContent = number(selected) + ' · ' + mission.title;
    find('[data-file]').textContent = 'OP.' + number(selected);
    find('[data-title]').textContent = mission.title;
    find('[data-rating]').textContent = mission.stage
      ? '伊斯特拉 ' + ratingText(stageRating(mission)) + '　莫斯科城区 ' + ratingText(stars[selected])
      : mission.href ? '最高评级 ' + ratingText(stars[selected]) : '未开放 · 可查看简报';
    find('[data-objective]').textContent = mission.objective;
    find('[data-description]').textContent = mission.description;
    find('[data-next]').textContent = mission.date;
    find('[data-branch]').hidden = !mission.stage;
    find('[data-branch]').textContent = mission.stage || '';
    const stageLink = find('[data-stage]');
    if (stageLink) {
      stageLink.hidden = !mission.stageHref;
      if (mission.stageHref) stageLink.href = mission.stageHref;
    }
    const classicChapter = find('[data-classic-chapter]');
    if (classicChapter) {
      classicChapter.hidden = !(mission.classic && readSave(mission.classic));
      if (!classicChapter.hidden) classicChapter.href = mission.classicHref;
    }
    enter.disabled = !mission.href;
    enter.querySelector('span').textContent = mission.stage ? '进入伊斯特拉阶段' : mission.href ? '进入战场' : '未开放';
    find('[data-note]').textContent = mission.endpoint
      ? (endpoints.every(Boolean) ? '南北端点均已达成 · A–A 线战役完成' : 'A–A 线战果 · '+endpoints.filter(Boolean).length+' / '+endpoints.length+' 端点')
      : mission.href ? '本战场独立保存' : '可查看简报';
    for (const [selector, key] of [
      ['[data-classic]', 'iron-snow-german-01-v6'],
      ['[data-extended]', 'iron-snow-german-01-v7'],
      ['[data-historical-legacy]', 'iron-snow-soviet-01-v5'],
      ['[data-legacy]', 'iron-snow-soviet-01-v4'],
    ]) {
      const link = find(selector);
      if (link) link.hidden = selected !== 0 || !readSave(key);
    }
  }

  const completed = stars.filter(Boolean).length;
  const openCount = missions.filter(mission => mission.href).length;
  find('.gc-progress b').textContent = String(completed).padStart(2, '0');
  find('[data-total]').textContent = openCount;
  const availability = find('[data-node-summary]');
  if (availability) availability.textContent = '已开放 ' + openCount + ' 关 · ' + (missions.length - openCount) + ' 关可查看简报';
  find('[data-enter]').onclick = () => {
    const href = missions[selected].href;
    if (href) location.assign(href);
  };
  find('[data-back]').onclick = () => location.assign('./start.html#campaign');
  render();
})();
