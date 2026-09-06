import { defineConfig } from 'astro/config';
import starlight from '@astrojs/starlight';

const product = (label, items) => ({ label, items });

export default defineConfig({
  site: 'https://tamawish.github.io', base: '/ResourceWorldResetter', output: 'static',
  integrations: [starlight({
    title: 'ResourceWorldResetter', description: 'Safe, observable resource-world regeneration for Minecraft servers.',
    favicon: '/favicon.svg',
    social: [{ icon: 'github', label: 'GitHub', href: 'https://github.com/TamaWish/ResourceWorldResetter' }],
    customCss: ['./src/styles/custom.css'],
    components: { SocialIcons: './src/components/SocialIcons.astro', Sidebar: './src/components/Sidebar.astro' },
    sidebar: [
      product('ResourceWorldResetter', [
        { label: 'Overview', slug: '' }, { label: 'RWR wiki home', slug: 'rwr' },
        { label: 'Installation', slug: 'rwr/installation' }, { label: 'Configuration', slug: 'rwr/configuration' },
        { label: 'Commands & permissions', slug: 'rwr/commands-and-permissions' }, { label: 'Reset lifecycle', slug: 'rwr/reset-lifecycle' },
        { label: 'Schedules & evacuation', slug: 'rwr/schedules-and-evacuation' }, { label: 'Recovery & troubleshooting', slug: 'rwr/recovery-and-troubleshooting' },
        { label: 'FAQ', slug: 'rwr/faq' },
      ]),
      product('PlaceholderAPI', [
        { label: 'Overview', slug: 'placeholderapi' }, { label: 'Installation', slug: 'placeholderapi/installation' },
        { label: 'Placeholder reference', slug: 'placeholderapi/placeholders' }, { label: 'Configuration', slug: 'placeholderapi/configuration' },
        { label: 'Commands & troubleshooting', slug: 'placeholderapi/troubleshooting' },
      ]),
      product('Discord Webhook', [
        { label: 'Overview', slug: 'discord' }, { label: 'Installation', slug: 'discord/installation' },
        { label: 'Configuration', slug: 'discord/configuration' }, { label: 'Delivery & retries', slug: 'discord/delivery' },
        { label: 'Commands & troubleshooting', slug: 'discord/troubleshooting' },
      ]),
      product('Prometheus', [
        { label: 'Overview', slug: 'prometheus' }, { label: 'Installation', slug: 'prometheus/installation' },
        { label: 'Exporter configuration', slug: 'prometheus/configuration' }, { label: 'Metrics & PromQL', slug: 'prometheus/metrics' },
        { label: 'Grafana & Docker', slug: 'prometheus/grafana' }, { label: 'Security & troubleshooting', slug: 'prometheus/troubleshooting' },
      ]),
      product('Public API', [
        { label: 'Overview', slug: 'api' }, { label: 'Dependency setup', slug: 'api/dependency' },
        { label: 'Service discovery', slug: 'api/service-discovery' }, { label: 'Snapshots', slug: 'api/snapshots' },
        { label: 'Events', slug: 'api/events' }, { label: 'Threading & lifecycle', slug: 'api/threading-lifecycle' },
        { label: 'Compatibility', slug: 'api/compatibility' },
      ]),
      product('Reference', [
        { label: 'Downloads', slug: 'download' }, { label: 'Operations & migration', slug: 'reference/operations-and-migration' },
        { label: 'Release notes', slug: 'reference/release-notes' }, { label: 'Integration development', slug: 'reference/development' },
      ]),
    ],
  })],
});
