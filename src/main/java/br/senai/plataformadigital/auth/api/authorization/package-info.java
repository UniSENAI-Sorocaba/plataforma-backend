/**
 * API pública de preparação da autorização, sem política de papéis de negócio.
 * Adapters futuros devem fornecer identidade autenticada e política aprovada
 * a partir de fontes confiáveis do servidor. O adapter HTTP aplica o catálogo
 * explícito de rotas e nega acesso quando não existe política institucional.
 */
package br.senai.plataformadigital.auth.api.authorization;
