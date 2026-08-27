(() => {
  const navToggle = document.querySelector('[data-nav-toggle]');
  const navLinks = document.querySelector('[data-nav-links]');
  if (navToggle && navLinks) {
    navToggle.addEventListener('click', () => {
      const open = navLinks.classList.toggle('open');
      navToggle.setAttribute('aria-expanded', String(open));
    });
    navLinks.addEventListener('click', event => {
      if (event.target.closest('a')) {
        navLinks.classList.remove('open');
        navToggle.setAttribute('aria-expanded', 'false');
      }
    });
  }

  document.querySelectorAll('[data-copy]').forEach(button => {
    button.addEventListener('click', async () => {
      const target = document.querySelector(button.dataset.copy);
      if (!target) return;
      const original = button.textContent;
      try {
        await navigator.clipboard.writeText(target.textContent.trim());
        button.textContent = 'Copied';
      } catch {
        const selection = window.getSelection();
        const range = document.createRange();
        range.selectNodeContents(target);
        selection.removeAllRanges();
        selection.addRange(range);
        button.textContent = 'Select text';
      }
      window.setTimeout(() => { button.textContent = original; }, 1800);
    });
  });

  const search = document.querySelector('[data-doc-search]');
  if (search) {
    const sections = [...document.querySelectorAll('[data-searchable]')];
    const empty = document.querySelector('[data-search-empty]');
    search.addEventListener('input', () => {
      const query = search.value.trim().toLowerCase();
      let matches = 0;
      sections.forEach(section => {
        const show = !query || section.textContent.toLowerCase().includes(query);
        section.hidden = !show;
        if (show) matches += 1;
      });
      empty?.classList.toggle('visible', matches === 0);
    });
  }

  const filters = [...document.querySelectorAll('[data-release-filter]')];
  if (filters.length) {
    const releases = [...document.querySelectorAll('[data-release]')];
    filters.forEach(filter => filter.addEventListener('click', () => {
      filters.forEach(item => item.classList.remove('active'));
      filter.classList.add('active');
      const value = filter.dataset.releaseFilter;
      releases.forEach(release => {
        release.hidden = value !== 'all' && release.dataset.release !== value;
      });
    }));
  }
})();
