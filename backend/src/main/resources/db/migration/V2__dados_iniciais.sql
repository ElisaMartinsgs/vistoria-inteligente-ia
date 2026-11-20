-- Unidades de exemplo da rede Sabor & Cia
INSERT INTO unidade (nome, endereco, cidade, uf) VALUES
    ('Sabor & Cia - Boa Viagem', 'Av. Boa Viagem, 1200', 'Recife', 'PE'),
    ('Sabor & Cia - Recife Antigo', 'Rua do Bom Jesus, 150', 'Recife', 'PE'),
    ('Sabor & Cia - Olinda', 'Av. Getúlio Vargas, 900', 'Olinda', 'PE');

-- Normas usadas no checklist. NR-2 e NR-27 foram revogadas e servem para testar a regra "norma inativa".
INSERT INTO norma_regulamentadora (codigo, titulo, ativa, observacao) VALUES
    ('NR-6',  'Equipamento de Proteção Individual - EPI', TRUE,  NULL),
    ('NR-8',  'Edificações (pisos, escadas, rampas)', TRUE,  NULL),
    ('NR-10', 'Segurança em Instalações e Serviços em Eletricidade', TRUE,  NULL),
    ('NR-12', 'Segurança no Trabalho em Máquinas e Equipamentos', TRUE,  NULL),
    ('NR-23', 'Proteção contra Incêndios', TRUE,  NULL),
    ('NR-24', 'Condições Sanitárias e de Conforto nos Locais de Trabalho', TRUE,  NULL),
    ('NR-26', 'Sinalização de Segurança', TRUE,  NULL),
    ('NR-2',  'Inspeção Prévia', FALSE, 'Revogada em 2019'),
    ('NR-27', 'Registro Profissional do Técnico de Segurança do Trabalho', FALSE, 'Revogada em 2008');
