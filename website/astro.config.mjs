import { defineConfig } from 'astro/config';
import starlight from '@astrojs/starlight';

export default defineConfig({
  site: 'https://tamawish.github.io',
  base: '/ResourceWorldResetter',
  output: 'static',
  integrations: [
    starlight({
      title: 'ResourceWorldResetter',
      description: 'Safe, observable resource-world regeneration for Minecraft servers.',
      favicon: '/favicon.svg',
      social: [
        {
          icon: 'github',
          label: 'GitHub',
          href: 'https://github.com/TamaWish/ResourceWorldResetter',
        },
      ],
      customCss: ['./src/styles/custom.css'],
      components: {
        SocialIcons: './src/components/SocialIcons.astro',
      },
      sidebar: [
        { label: 'Start here', items: [
          { label: 'Overview', slug: '' },
          { label: 'Install RWR', slug: 'getting-started' },
        ] },
        { label: 'Operator guide', items: [
          { label: 'Configuration', slug: 'operator/configuration' },
          { label: 'Commands & permissions', slug: 'operator/commands-and-permissions' },
          { label: 'Reset lifecycle', slug: 'operator/reset-lifecycle' },
          { label: 'Schedules & evacuation', slug: 'operator/schedules-and-evacuation' },
          { label: 'Recovery & troubleshooting', slug: 'operator/recovery-and-troubleshooting' },
          { label: 'FAQ', slug: 'operator/faq' },
        ] },
        { label: 'Add-on wikis', items: [
          { label: 'All add-ons', slug: 'addons' },
          { label: 'PlaceholderAPI', slug: 'addons/placeholderapi' },
          { label: 'Discord Webhook', slug: 'addons/discord-webhook' },
          { label: 'Prometheus', slug: 'addons/prometheus' },
          { label: 'Public API', slug: 'addons/api' },
        ] },
        { label: 'Reference', items: [
          { label: 'Operations & migration', slug: 'reference/operations-and-migration' },
          { label: 'Release notes', slug: 'reference/release-notes' },
          { label: 'Integration development', slug: 'reference/development' },
        ] },
      ],
    }),
  ],
});
