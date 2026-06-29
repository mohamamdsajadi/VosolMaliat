/** @type {import('next').NextConfig} */
const apiBaseUrl = process.env.API_BASE_URL || 'http://localhost:8025';

const nextConfig = {
  reactStrictMode: false,
  async rewrites() {
    return [
      {
        source: '/api/:path*',
        destination: `${apiBaseUrl}/api/:path*`,
      },
    ]
  },
}


module.exports = nextConfig
